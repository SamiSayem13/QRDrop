# 📸 Photo Transfer Using QR Code

A simple photo transfer system that transfers images from a Windows PC to an Android device using QR codes.

The project uses a **Python sender** on the PC and a **Flutter Android receiver**. The selected image is compressed into JPEG data, divided into smaller chunks, converted into QR codes, and then scanned by the Android application.

The Android app collects all QR data, reconstructs the original JPEG image, and displays the received photo.

---

## ✨ Features

- 🖼️ Select an image from the Windows PC
- 📦 Compress the image before transmission
- 🔐 Convert compressed image data into Base64
- 🔳 Split the data into multiple QR codes
- 📱 Scan QR codes using the Flutter Android application
- 🔄 Supports QR codes arriving out of order
- 🚫 Ignores duplicate QR frames
- 📊 Shows transfer progress
- 🧩 Reconstructs the complete JPEG image
- 🖼️ Displays the received image on Android
- 💾 Saves the received image to the Android device

---

## 🏗️ How It Works

```text
                WINDOWS PC
                    │
                    ▼
              Select Image
                    │
                    ▼
             JPEG Compression
                    │
                    ▼
             Split Into Chunks
                    │
                    ▼
              Base64 Encoding
                    │
                    ▼
               QR Generation
                    │
             ┌──────┴──────┐
             ▼             ▼
           QR #1         QR #2 ... QR #N
             │             │
             └──────┬──────┘
                    │
                    ▼
              ANDROID PHONE
                    │
                    ▼
             Flutter QR Scanner
                    │
                    ▼
             Collect QR Chunks
                    │
                    ▼
          Reconstruct JPEG Bytes
                    │
                    ▼
              Decode JPEG
                    │
                    ▼
             Display Image
                    │
                    ▼
              Save to Gallery
