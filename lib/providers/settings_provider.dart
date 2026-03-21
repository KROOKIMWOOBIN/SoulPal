import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

class SettingsProvider extends ChangeNotifier {
  static const _localeKey = 'soulpal_locale';

  final SharedPreferences _prefs;
  String _locale;

  SettingsProvider(this._prefs)
      : _locale = _prefs.getString(_localeKey) ?? 'ko';

  String get locale => _locale;
  bool get isKorean => _locale.startsWith('ko');

  String t(String ko, String en) => isKorean ? ko : en;

  Future<void> setLocale(String locale) async {
    _locale = locale;
    await _prefs.setString(_localeKey, locale);
    notifyListeners();
  }
}
