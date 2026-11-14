# آسان قرآنی قطعے — Android app

One Quranic word per screen, taken from the book *آسان قرآنی قطعے* (451 words).

## What the app does
- Cover → word 1 … word 451 → author's note → summary
- Swipe like an Urdu book (next word comes from the left), or use **اگلا / پچھلا**
- Tap the number at the bottom (e.g. `نمبر 12 / 451`) to jump to any word
- Pinch to zoom, double-tap to zoom in/out
- Remembers the last word you were on
- Works fully offline (all 451 cards are inside the app)

## Run it
1. Unzip the folder and open it in **Android Studio** (File → Open → select `QuraniQate`).
2. Wait for the Gradle sync to finish (first time needs internet; it downloads Gradle and libraries).
   If Android Studio offers to upgrade the Android Gradle Plugin, you can accept.
3. Pick an emulator or your phone (USB debugging on) and press **Run ▶**.
4. To share with others: **Build → Build App Bundle(s)/APK(s) → Build APK(s)** and send `app-debug.apk`.

## Put it on GitHub
```
git init
git add .
git commit -m "First version of Qurani Qate app"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/QuraniQate.git
git push -u origin main
```

## Where things are
- `app/src/main/assets/cards/w001.webp … w451.webp` — one image per word (number = book number)
- `app/src/main/assets/pages/` — cover, note, summary
- `app/src/main/java/com/shoaib/qurani/MainActivity.kt` — all app code
- To make swiping left-to-right instead, change `LayoutDirection.Rtl` to `LayoutDirection.Ltr` in `MainActivity.kt`.

## Download the app
Latest APK (built automatically by GitHub on every change):
https://github.com/MSM-official/Quran-Asaan/releases/latest/download/Quran-Asaan.apk
