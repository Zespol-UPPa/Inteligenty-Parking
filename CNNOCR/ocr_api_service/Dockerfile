FROM python:3.11-slim

WORKDIR /app

# Install system dependencies
# Note: libgl1-mesa-glx is replaced by libgl1 in newer Debian versions (Trixie+)
RUN apt-get update && apt-get install -y \
    libgl1 \
    libglib2.0-0 \
    libsm6 \
    libxext6 \
    libxrender-dev \
    libgomp1 \
    libgthread-2.0-0 \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Copy requirements and install Python dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy application code
COPY . .

# Create directory for model and test images
RUN mkdir -p /app/CNNOCR /app/test_images

# Note: Model file (best.pt) will be mounted via volume in docker-compose.yml
# This ensures the model is available at /app/CNNOCR/best.pt

# Expose port
EXPOSE 5000

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=120s --retries=3 \
    CMD curl -f http://localhost:5000/health || exit 1

# Run application
CMD ["python", "app.py"]

