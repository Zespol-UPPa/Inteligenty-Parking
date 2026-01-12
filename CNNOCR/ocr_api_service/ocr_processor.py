import cv2
import numpy as np
from ultralytics import YOLO
import easyocr
from typing import List, Tuple, Optional
import logging
import torch
import torch.serialization
import re

logger = logging.getLogger(__name__)


def is_valid_license_plate(plate_text: str) -> bool:
    """
    Waliduje czy tekst wygląda jak prawdziwa polska tablica rejestracyjna
    
    Polskie formaty:
    - Stary: ABC1234 (3 litery + 4 cyfry) - 7 znaków
    - Nowy: AB123CD (2 litery + 3 cyfry + 2 litery) - 7 znaków
    - Wariant: ABC12CD (3 litery + 2 cyfry + 2 litery) - 7 znaków
    - Możliwe też: AB1234 (2 litery + 4 cyfry) - 6 znaków (starsze)
    
    Args:
        plate_text: Tekst do walidacji
        
    Returns:
        True jeśli wygląda jak tablica rejestracyjna, False w przeciwnym razie
    """
    if not plate_text:
        return False
    
    # Usuń białe znaki i zamień na wielkie litery
    plate = plate_text.strip().upper().replace(" ", "").replace("-", "")
    
    # Minimalna długość: 5 znaków, maksymalna: 8 znaków
    if len(plate) < 5 or len(plate) > 8:
        return False
    
    # Odfiltruj oczywiste błędy (tylko litery lub tylko cyfry)
    if plate.isalpha() or plate.isdigit():
        return False
    
    # Odfiltruj bardzo krótkie teksty (WE, PL, itp.)
    if len(plate) <= 2:
        return False
    
    # Odfiltruj teksty które są tylko cyframi z jedną literą na końcu (np. "0456C")
    if len(plate) <= 5 and plate[:-1].isdigit() and plate[-1].isalpha():
        return False
    
    # Sprawdź czy zawiera przynajmniej 2 litery i 2 cyfry
    letters = sum(1 for c in plate if c.isalpha())
    digits = sum(1 for c in plate if c.isdigit())
    
    if letters < 2 or digits < 2:
        return False
    
    # Wzorce dla polskich tablic rejestracyjnych
    patterns = [
        r'^[A-Z]{2,3}\d{2,4}[A-Z]{0,2}$',  # AB123CD, ABC1234, ABC12CD
        r'^[A-Z]{2}\d{3,4}$',              # AB1234, AB123
        r'^[A-Z]{3}\d{4}$',                # ABC1234 (stary format)
    ]
    
    for pattern in patterns:
        if re.match(pattern, plate):
            return True
    
    # Jeśli nie pasuje do żadnego wzorca, ale ma odpowiednią strukturę (litery+cyfry+litery)
    # i odpowiednią długość, zaakceptuj (może być niestandardowy format)
    if 6 <= len(plate) <= 8 and letters >= 2 and digits >= 2:
        # Sprawdź czy nie zaczyna się od samych cyfr (błąd OCR)
        if plate[0].isdigit():
            return False
        # Sprawdź czy nie kończy się tylko cyframi (może być OK, ale sprawdź strukturę)
        return True
    
    return False


# Fix dla PyTorch 2.6+ - pozwól na ładowanie modeli Ultralytics
# PyTorch 2.6 zmienił domyślne zachowanie torch.load na weights_only=True
# Modele YOLO wymagają weights_only=False
try:
    from ultralytics.nn.tasks import DetectionModel
    # Dodaj DetectionModel do bezpiecznej listy globalnych
    if hasattr(torch.serialization, 'add_safe_globals'):
        torch.serialization.add_safe_globals([DetectionModel])
        logger.info("Added DetectionModel to PyTorch safe globals for PyTorch 2.6+ compatibility")
except (ImportError, AttributeError) as e:
    # Fallback - starsze wersje PyTorch nie mają tego problemu
    logger.debug(f"PyTorch 2.6+ compatibility fix not needed: {e}")


class OCRProcessor:
    def __init__(self, model_path: str, gpu: bool = False):
        """
        Inicjalizuje modele YOLO i easyOCR
        
        Args:
            model_path: Ścieżka do modelu YOLO (best.pt)
            gpu: Czy używać GPU (wymaga CUDA)
        """
        logger.info(f"Initializing OCRProcessor with model: {model_path}, GPU: {gpu}")
        logger.info(f"PyTorch version: {torch.__version__}")
        
        try:
            # Workaround dla PyTorch 2.6+ - tymczasowo ustaw weights_only=False dla YOLO
            # Ultralytics YOLO używa torch.load wewnętrznie i wymaga weights_only=False
            original_load = torch.load
            torch_version_parts = torch.__version__.split('.')
            major_version = int(torch_version_parts[0])
            minor_version = int(torch_version_parts[1]) if len(torch_version_parts) > 1 else 0
            
            if major_version >= 2 and minor_version >= 6:
                # PyTorch 2.6+ wymaga specjalnego podejścia
                def patched_load(*args, **kwargs):
                    # Usuń weights_only z kwargs jeśli istnieje, aby użyć domyślnego False
                    kwargs.pop('weights_only', None)
                    # Dodaj weights_only=False explicite
                    return original_load(*args, weights_only=False, **kwargs)
                
                torch.load = patched_load
                try:
                    self.yolo_model = YOLO(model_path)
                    logger.info("YOLO model loaded successfully (with PyTorch 2.6+ compatibility)")
                finally:
                    torch.load = original_load  # Przywróć oryginalną funkcję
            else:
                # Starsze wersje PyTorch - normalne ładowanie
                self.yolo_model = YOLO(model_path)
                logger.info("YOLO model loaded successfully")
                
        except Exception as e:
            logger.error(f"Failed to load YOLO model: {e}", exc_info=True)
            raise
        
        try:
            self.easyocr_reader = easyocr.Reader(['en'], gpu=gpu)
            logger.info("EasyOCR reader initialized successfully")
        except Exception as e:
            logger.error(f"Failed to initialize EasyOCR reader: {e}")
            raise
        
        self.confidence_threshold = 0.4  # Minimalne confidence dla YOLO
        
    def detect_and_read(self, image: np.ndarray) -> List[Tuple[str, float, Tuple[int, int, int, int]]]:
        """
        Wykrywa tablice rejestracyjne w obrazie i odczytuje je
        
        Args:
            image: Obraz w formacie BGR (OpenCV)
            
        Returns:
            Lista krotek (plate_text, confidence, bbox) gdzie:
            - plate_text: Odczytany tekst tablicy
            - confidence: Poziom pewności (0-1)
            - bbox: (x1, y1, x2, y2) bounding box
        """
        results = []
        
        try:
            # Wykryj tablice przez YOLO
            yolo_results = self.yolo_model(image, conf=self.confidence_threshold)
            
            for r in yolo_results:
                for box in r.boxes.xyxy:
                    x1, y1, x2, y2 = map(int, box)
                    
                    # Wyciągnij region tablicy
                    plate_roi = image[y1:y2, x1:x2]
                    
                    if plate_roi.size == 0:
                        continue
                    
                    # Odczytaj tablicę przez easyOCR
                    plate_text, confidence = self._read_plate_easyocr(plate_roi)
                    
                    if plate_text:
                        results.append((plate_text, confidence, (x1, y1, x2, y2)))
                        logger.info(f"Detected plate: {plate_text} with confidence: {confidence:.2f}")
        
        except Exception as e:
            logger.error(f"Error in detect_and_read: {e}", exc_info=True)
        
        return results
    
    def _read_plate_easyocr(self, plate_roi: np.ndarray) -> Tuple[Optional[str], float]:
        """
        Odczytuje tekst z tablicy rejestracyjnej używając easyOCR
        
        Args:
            plate_roi: Region obrazu z tablicą (BGR)
            
        Returns:
            Krotka (plate_text, confidence) lub (None, 0.0) jeśli nie udało się odczytać
        """
        try:
            # Preprocessing
            gray = cv2.cvtColor(plate_roi, cv2.COLOR_BGR2GRAY)
            gray = cv2.resize(gray, None, fx=2, fy=2, interpolation=cv2.INTER_CUBIC)
            
            # Odczytaj tekst
            ocr_results = self.easyocr_reader.readtext(
                gray,
                allowlist="ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789",
                detail=1
            )
            
            if not ocr_results:
                return None, 0.0
            
            # Wybierz wynik z najwyższym confidence
            best_result = max(ocr_results, key=lambda x: x[2])
            text = best_result[1].replace(" ", "").replace("\n", "")
            confidence = best_result[2]
            
            # WALIDACJA: Sprawdź czy tekst wygląda jak tablica rejestracyjna
            if not is_valid_license_plate(text):
                logger.warning(f"Rejected invalid license plate format: '{text}' (confidence: {confidence:.2f})")
                return None, 0.0
            
            return text, confidence
            
        except Exception as e:
            logger.error(f"Error reading plate with easyOCR: {e}", exc_info=True)
            return None, 0.0






