import 'dart:ui';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'app.dart';
import 'providers/character_provider.dart';
import 'providers/chat_provider.dart';
import 'providers/settings_provider.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  // ── 1. Build 예외 → 회색 빈 화면 대신 에러 카드 표시 ──────────────
  ErrorWidget.builder = (FlutterErrorDetails details) {
    return Material(
      color: Colors.transparent,
      child: Container(
        color: const Color(0xFF1A1525),
        alignment: Alignment.center,
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.error_outline_rounded,
                color: Colors.redAccent, size: 48),
            const SizedBox(height: 12),
            const Text(
              '[Beta] 화면을 불러오지 못했습니다',
              style: TextStyle(
                  color: Colors.white,
                  fontSize: 16,
                  fontWeight: FontWeight.w600),
            ),
            const SizedBox(height: 8),
            Text(
              details.exception.toString(),
              style: const TextStyle(color: Colors.white54, fontSize: 11),
              textAlign: TextAlign.center,
              maxLines: 6,
              overflow: TextOverflow.ellipsis,
            ),
          ],
        ),
      ),
    );
  };

  // ── 2. Flutter 프레임워크 에러 (렌더링·레이아웃 등) ────────────────
  FlutterError.onError = (FlutterErrorDetails details) {
    FlutterError.presentError(details);
    // SnackBar는 앱이 완전히 뜬 이후에만 동작 — 조용히 누락되어도 괜찮음
    WidgetsBinding.instance.addPostFrameCallback((_) {
      showGlobalError(details.exception.toString());
    });
  };

  // ── 3. Dart async / isolate 에러 ─────────────────────────────────
  PlatformDispatcher.instance.onError = (error, stack) {
    debugPrint('[PlatformDispatcher] $error\n$stack');
    showGlobalError(error.toString());
    return true; // handled
  };

  await SystemChrome.setPreferredOrientations([
    DeviceOrientation.portraitUp,
    DeviceOrientation.portraitDown,
  ]);

  final prefs = await SharedPreferences.getInstance();

  runApp(
    MultiProvider(
      providers: [
        ChangeNotifierProvider(create: (_) => SettingsProvider(prefs)),
        ChangeNotifierProvider(create: (_) => CharacterProvider(prefs)),
        ChangeNotifierProvider(create: (_) => ChatProvider(prefs)),
      ],
      child: const SoulPalApp(),
    ),
  );
}
