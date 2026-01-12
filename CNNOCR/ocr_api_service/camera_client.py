import cv2
import numpy as np
from typing import Optional
import os
import glob
import random
import logging

logger = logging.getLogger(__name__)


class CameraClient:
    def __init__(self, camera_url: Optional[str] = None, local_path: Optional[str] = None):
        """
        Klient do pobierania obrazów z kamery
        
        Args:
            camera_url: URL do kamery IP (np. "rtsp://..." lub indeks kamery USB)
            local_path: Ścieżka do lokalnych obrazów testowych (dla symulacji)
        """
        self.camera_url = camera_url
        self.local_path = local_path
        self.cap = None
        self.image_files = []
        
        if camera_url:
            # Sprawdź czy to numer (USB camera) czy URL (IP camera)
            try:
                camera_index = int(camera_url)
                self.cap = cv2.VideoCapture(camera_index)
                logger.info(f"Initialized USB camera with index: {camera_index}")
            except ValueError:
                # To jest URL (np. rtsp://...)
                self.cap = cv2.VideoCapture(camera_url)
                logger.info(f"Initialized IP camera with URL: {camera_url}")
            
            if not self.cap.isOpened():
                logger.warning(f"Failed to open camera: {camera_url}")
                self.cap = None
                
        elif local_path:
            # Dla testów - będzie używać lokalnych obrazów
            if os.path.exists(local_path):
                self.image_files = glob.glob(os.path.join(local_path, "*.jpg"))
                self.image_files.extend(glob.glob(os.path.join(local_path, "*.png")))
                self.image_files.extend(glob.glob(os.path.join(local_path, "*.jpeg")))
                logger.info(f"Found {len(self.image_files)} test images in {local_path}")
            else:
                logger.warning(f"Local path does not exist: {local_path}")
            self.current_index = 0
    
    def capture_image(self) -> Optional[np.ndarray]:
        """Pobiera obraz z kamery"""
        if self.cap:
            # Pobierz z kamery IP/USB
            ret, frame = self.cap.read()
            if ret:
                return frame
            else:
                logger.warning("Failed to capture frame from camera")
                return None
        elif self.local_path and self.image_files:
            # Dla testów - zwróć losowy lokalny obraz
            if self.current_index >= len(self.image_files):
                self.current_index = 0
            image_path = self.image_files[self.current_index]
            self.current_index += 1
            image = cv2.imread(image_path)
            if image is not None:
                logger.debug(f"Loaded test image: {image_path}")
            return image
        else:
            # Brak źródła obrazu
            logger.warning("No camera or test images available")
            return None
    
    def release(self):
        """Zwalnia zasoby kamery"""
        if self.cap:
            self.cap.release()
            self.cap = None
            logger.info("Camera released")







