import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'core/theme/app_theme.dart';
import 'features/splash/splash_screen.dart';
import 'providers/settings_provider.dart';

class SoulPalApp extends StatelessWidget {
  const SoulPalApp({super.key});

  @override
  Widget build(BuildContext context) {
    final settings = context.watch<SettingsProvider>();

    return MaterialApp(
      title: 'SoulPal',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.lightTheme,
      darkTheme: AppTheme.darkTheme,
      themeMode: settings.themeMode,
      locale: Locale(settings.locale),
      supportedLocales: const [Locale('ko'), Locale('en')],
      home: const SplashScreen(),
    );
  }
}
