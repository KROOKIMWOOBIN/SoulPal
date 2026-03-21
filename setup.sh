#!/bin/bash
# SoulPal Flutter Project Setup Script
# Run this after installing Flutter SDK

set -e

echo "🌟 SoulPal Setup Starting..."

# Check Flutter
if ! command -v flutter &> /dev/null; then
  echo "❌ Flutter not found. Install Flutter first:"
  echo "   https://docs.flutter.dev/get-started/install/macos"
  exit 1
fi

echo "✅ Flutter found: $(flutter --version | head -1)"

# Backup existing lib files
echo "📦 Backing up source files..."
cp -r lib /tmp/soulpal_lib_backup 2>/dev/null || true

# Create Flutter scaffold (generates android/, ios/, etc.)
echo "🔧 Creating Flutter project scaffold..."
flutter create --project-name soulpal --org com.soulpal --platforms android,ios,macos,windows .

# Restore our custom source files
echo "🔄 Restoring custom source files..."
cp -r /tmp/soulpal_lib_backup/. lib/

# Get dependencies
echo "📥 Installing dependencies..."
flutter pub get

# Android: Setup for Ollama localhost access via ADB
echo ""
echo "✅ Setup complete!"
echo ""
echo "📱 To run on Android emulator:"
echo "   flutter run"
echo ""
echo "📱 To run on physical Android device with Ollama:"
echo "   adb reverse tcp:11434 tcp:11434"
echo "   flutter run"
echo ""
echo "🤖 Make sure Ollama is running:"
echo "   ollama serve"
echo "   ollama pull llama3"
