from flask import Flask, request, jsonify
import cv2
import numpy as np
from ocr_processor import OCRProcessor, is_valid_license_plate
from camera_client import CameraClient
import requests
import os
from datetime import datetime
from typing import Optional
import logging

app = Flask(__name__)
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# Konfiguracja z zmiennych środowiskowych
OCR_SERVICE_URL = os.getenv("OCR_SERVICE_URL", "http://ocr-service:8086")
PARKING_ID = int(os.getenv("PARKING_ID", "1"))  # ID parkingu
CAMERA_ID = int(os.getenv("CAMERA_ID", "1"))    # ID kamery
DIRECTION = os.getenv("DIRECTION", "entry")     # "entry" lub "exit"
MODEL_PATH = os.getenv("MODEL_PATH", "/app/CNNOCR/best.pt")
USE_GPU = os.getenv("USE_GPU", "false").lower() == "true"

# Inicjalizacja procesora OCR
try:
    ocr_processor = OCRProcessor(
        model_path=MODEL_PATH,
        gpu=USE_GPU
    )
    logger.info("OCR processor initialized successfully")
except Exception as e:
    logger.error(f"Failed to initialize OCR processor: {e}")
    ocr_processor = None

# Klient kamery (dla symulacji można użyć lokalnych obrazów)
camera_client = CameraClient(
    camera_url=os.getenv("CAMERA_URL", None),  # URL do kamery IP lub indeks USB
    local_path=os.getenv("CAMERA_LOCAL_PATH", None)  # Dla testów z lokalnych plików
)


@app.route("/health", methods=["GET"])
def health():
    """Health check endpoint"""
    status = {
        "status": "ok",
        "ocr_processor": "initialized" if ocr_processor is not None else "failed",
        "camera": "available" if camera_client.cap is not None or camera_client.image_files else "unavailable"
    }
    return jsonify(status), 200


@app.route("/process-image", methods=["POST"])
def process_image():
    """
    Endpoint który przyjmuje obraz (multipart/form-data) lub URL do obrazu
    i przetwarza go przez YOLO + easyOCR
    
    Parametry:
    - image: plik obrazu (multipart/form-data)
    - image_url: URL do obrazu (JSON)
    - direction: "entry" lub "exit" (opcjonalny, z form data lub query param, domyślnie ze zmiennej środowiskowej)
    """
    if ocr_processor is None:
        return jsonify({"error": "OCR processor not initialized"}), 500
    
    try:
        # Pobierz direction z requesta (form data, query param lub JSON) - priorytet: form > query > JSON > env
        direction = None
        if request.form and "direction" in request.form:
            direction = request.form.get("direction")
        elif request.args and "direction" in request.args:
            direction = request.args.get("direction")
        elif request.is_json and "direction" in request.json:
            direction = request.json.get("direction")
        
        # Walidacja direction
        if direction and direction.lower() not in ["entry", "exit"]:
            logger.warning(f"Invalid direction '{direction}', using default: {DIRECTION}")
            direction = None
        
        # Użyj direction z requesta lub domyślnego ze zmiennej środowiskowej
        effective_direction = direction.lower() if direction else DIRECTION
        logger.info(f"Processing image with direction: {effective_direction}")
        
        image = None
        
        # Pobierz obraz z requesta
        if "image" in request.files:
            file = request.files["image"]
            image_bytes = file.read()
            nparr = np.frombuffer(image_bytes, np.uint8)
            image = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
            logger.info(f"Received image file: {file.filename}")
        elif request.is_json and "image_url" in request.json:
            # Pobierz obraz z URL
            import urllib.request
            try:
                resp = urllib.request.urlopen(request.json["image_url"], timeout=10)
                image_bytes = np.asarray(bytearray(resp.read()), dtype=np.uint8)
                image = cv2.imdecode(image_bytes, cv2.IMREAD_COLOR)
                logger.info(f"Loaded image from URL: {request.json['image_url']}")
            except Exception as e:
                logger.error(f"Failed to load image from URL: {e}")
                return jsonify({"error": f"Failed to load image from URL: {str(e)}"}), 400
        else:
            return jsonify({"error": "No image provided. Use 'image' file or 'image_url' in JSON"}), 400
        
        if image is None:
            return jsonify({"error": "Failed to decode image"}), 400
        
        # Przetwarzaj obraz przez YOLO + easyOCR
        results = ocr_processor.detect_and_read(image)
        
        if not results:
            logger.info("No license plate detected in image")
            return jsonify({"detected": False, "plates": [], "direction": effective_direction}), 200
        
        # Przetwarzaj każdą wykrytą tablicę
        detected_plates = []
        for plate_text, confidence, bbox in results:
            if plate_text and confidence > 0.5:  # Minimum confidence threshold
                # DODATKOWA WALIDACJA przed wysłaniem (podwójna walidacja - już w ocr_processor, ale dla pewności)
                if not is_valid_license_plate(plate_text):
                    logger.warning(f"Rejected invalid plate format: '{plate_text}' (confidence: {confidence:.2f})")
                    continue  # Pomiń tę tablicę
                
                logger.info(f"✅ Detected valid plate: {plate_text} (confidence: {confidence:.2f})")
                
                # Wysyłaj do OCR service (Spring Boot) z określonym direction
                send_result = send_to_ocr_service(plate_text, image, effective_direction)
                detected_plates.append({
                    "text": plate_text,
                    "confidence": float(confidence),
                    "bbox": bbox,
                    "sent_to_service": send_result
                })
        
        return jsonify({
            "detected": len(detected_plates) > 0,
            "plates": detected_plates,
            "direction": effective_direction
        }), 200
        
    except Exception as e:
        logger.error(f"Error processing image: {e}", exc_info=True)
        return jsonify({"error": str(e)}), 500


def send_to_ocr_service(plate_text: str, image: np.ndarray, direction: Optional[str] = None) -> bool:
    """
    Wysyła wynik OCR do Spring Boot OCR service
    
    Args:
        plate_text: Wykryty numer rejestracyjny
        image: Obraz (może być użyty do zapisania w przyszłości)
        direction: Kierunek ("entry" lub "exit"), jeśli None - użyje DIRECTION ze zmiennej środowiskowej
    """
    try:
        # Użyj direction z parametru lub domyślnego
        effective_direction = direction if direction else DIRECTION
        
        # Przygotuj payload zgodny z OcrEventDto
        payload = {
            "plate": plate_text,
            "timestamp": datetime.utcnow().isoformat() + "Z",
            "direction": effective_direction,
            "parkingId": PARKING_ID,
            "cameraId": CAMERA_ID,
            "imageUrl": None  # Można dodać URL do zapisanego obrazu w przyszłości
        }
        
        # Wyślij do OCR service
        response = requests.post(
            f"{OCR_SERVICE_URL}/ocr/webhook",
            json=payload,
            timeout=10
        )
        
        if response.status_code == 200:
            logger.info(f"Successfully sent plate {plate_text} to OCR service with direction: {effective_direction}")
            return True
        else:
            logger.error(f"Failed to send plate to OCR service: {response.status_code} - {response.text}")
            return False
            
    except Exception as e:
        logger.error(f"Error sending to OCR service: {e}", exc_info=True)
        return False


@app.route("/process-camera", methods=["POST"])
def process_camera():
    """
    Endpoint który pobiera obraz z kamery, przetwarza i wysyła wyniki
    """
    if ocr_processor is None:
        return jsonify({"error": "OCR processor not initialized"}), 500
    
    try:
        # Pobierz obraz z kamery
        image = camera_client.capture_image()
        
        if image is None:
            return jsonify({"error": "Failed to capture image from camera"}), 500
        
        # Przetwarzaj obraz
        results = ocr_processor.detect_and_read(image)
        
        if not results:
            return jsonify({"detected": False, "plates": []}), 200
        
        # Pobierz direction z requesta (query param lub JSON body) - podobnie jak w process_image
        direction = None
        if request.args and "direction" in request.args:
            direction = request.args.get("direction")
        elif request.is_json and "direction" in request.json:
            direction = request.json.get("direction")
        
        if direction and direction.lower() not in ["entry", "exit"]:
            direction = None
        
        effective_direction = direction.lower() if direction else DIRECTION
        
        # Wysyłaj każdą wykrytą tablicę
        detected_plates = []
        for plate_text, confidence, bbox in results:
            if plate_text and confidence > 0.5:
                # WALIDACJA przed wysłaniem
                if not is_valid_license_plate(plate_text):
                    logger.warning(f"Rejected invalid plate format: '{plate_text}' (confidence: {confidence:.2f})")
                    continue
                
                logger.info(f"✅ Detected valid plate: {plate_text} (confidence: {confidence:.2f})")
                
                send_result = send_to_ocr_service(plate_text, image, effective_direction)
                detected_plates.append({
                    "text": plate_text,
                    "confidence": float(confidence),
                    "bbox": bbox,
                    "sent_to_service": send_result
                })
        
        return jsonify({
            "detected": len(detected_plates) > 0,
            "plates": detected_plates,
            "direction": effective_direction
        }), 200
        
    except Exception as e:
        logger.error(f"Error processing camera: {e}", exc_info=True)
        return jsonify({"error": str(e)}), 500


if __name__ == "__main__":
    port = int(os.getenv("PORT", "5000"))
    app.run(host="0.0.0.0", port=port, debug=False)





