# 📸 Photo Transfer Using QR Code

First Extract the pythonfile.rar where we have the python codes and the android software to scan

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
```text

```
### QR Code Transfer Workflow

The application uses a **Sender → QR Show → Android Receiver** workflow to transfer image/file data through QR codes.

1. The **Sender** reads the original data directly without compressing or resizing it.
2. The data is divided into **1500-byte chunks**.
3. Each chunk is encoded using **Base64**.
4. Each chunk is converted into a QR code using the packet format:


PIXEL|frame_index|total_frames|width|height|base64_data


5. The generated QR frames are saved in the `GeneratedQr` folder.
6. **QR Show** displays the generated QR codes one by one in the correct frame order.
7. The **Android Receiver app** scans each QR code using the phone's camera.
8. The receiver collects the scanned frames using their `frame_index` and `total_frames` values.
9. After all frames are received, the Android app reconstructs the original data and restores the original image/file.

### Overall Flow

```text
Original File/Image
        ↓
     Sender
        ↓
  Split into Chunks
        ↓
     Base64
        ↓
   Generate QR Codes
        ↓
    QR Show
        ↓
  📱 Android Receiver
        ↓
    Scan QR Codes
        ↓
 Reconstruct Data
        ↓
 Original File/Image
```

