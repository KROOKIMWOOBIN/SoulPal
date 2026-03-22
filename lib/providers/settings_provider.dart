import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

class SettingsProvider extends ChangeNotifier {
  static const _localeKey = 'soulpal_locale';
  static const _themeModeKey = 'soulpal_theme_mode';
  static const _temperatureKey = 'soulpal_temperature';
  static const _contextLengthKey = 'soulpal_context_length';
  static const _historyCountKey = 'soulpal_history_count';
  static const _onboardingDoneKey = 'soulpal_onboarding_done';

  final SharedPreferences _prefs;
  String _locale;
  ThemeMode _themeMode;
  double _temperature;
  int _contextLength;
  int _historyCount;
  bool _onboardingDone;

  SettingsProvider(this._prefs)
      : _locale = _prefs.getString(_localeKey) ?? 'ko',
        _themeMode = ThemeMode.values[(_prefs.getInt(_themeModeKey) ?? 0)
                .clamp(0, ThemeMode.values.length - 1)],
        _temperature = _prefs.getDouble(_temperatureKey) ?? 0.8,
        _contextLength = _prefs.getInt(_contextLengthKey) ?? 2048,
        _historyCount = _prefs.getInt(_historyCountKey) ?? 10,
        _onboardingDone = _prefs.getBool(_onboardingDoneKey) ?? false;

  String get locale => _locale;
  bool get isKorean => _locale.startsWith('ko');
  ThemeMode get themeMode => _themeMode;
  double get temperature => _temperature;
  int get contextLength => _contextLength;
  int get historyCount => _historyCount;
  bool get onboardingDone => _onboardingDone;

  String t(String ko, String en) => isKorean ? ko : en;

  Future<void> setLocale(String locale) async {
    _locale = locale;
    await _prefs.setString(_localeKey, locale);
    notifyListeners();
  }

  Future<void> setThemeMode(ThemeMode mode) async {
    _themeMode = mode;
    await _prefs.setInt(_themeModeKey, mode.index);
    notifyListeners();
  }

  Future<void> setTemperature(double value) async {
    _temperature = value.clamp(0.1, 2.0);
    await _prefs.setDouble(_temperatureKey, _temperature);
    notifyListeners();
  }

  Future<void> setContextLength(int value) async {
    _contextLength = value;
    await _prefs.setInt(_contextLengthKey, value);
    notifyListeners();
  }

  Future<void> setHistoryCount(int value) async {
    _historyCount = value.clamp(1, 30);
    await _prefs.setInt(_historyCountKey, _historyCount);
    notifyListeners();
  }

  Future<void> completeOnboarding() async {
    _onboardingDone = true;
    await _prefs.setBool(_onboardingDoneKey, true);
    notifyListeners();
  }

  Future<void> resetLlmParams() async {
    _temperature = 0.8;
    _contextLength = 2048;
    _historyCount = 10;
    await _prefs.setDouble(_temperatureKey, _temperature);
    await _prefs.setInt(_contextLengthKey, _contextLength);
    await _prefs.setInt(_historyCountKey, _historyCount);
    notifyListeners();
  }
}
