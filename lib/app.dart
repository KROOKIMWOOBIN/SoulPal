import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'core/theme/app_theme.dart';
import 'features/splash/splash_screen.dart';
import 'providers/settings_provider.dart';

class SoulPalApp extends StatelessWidget {
  const SoulPalApp({super.key});

  @override
  Widget build(BuildContext context) {
    final locale = context.watch<SettingsProvider>().locale;

    return MaterialApp(
      title: 'SoulPal',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.lightTheme,
      locale: Locale(locale),
      supportedLocales: const [Locale('ko'), Locale('en')],
      home: const SplashScreen(),
    );
  }
}
