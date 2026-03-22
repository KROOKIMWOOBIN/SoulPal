import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:package_info_plus/package_info_plus.dart';

class UpdateInfo {
  final String latestVersion;
  final String releaseUrl;
  final String? releaseNotes;

  const UpdateInfo({
    required this.latestVersion,
    required this.releaseUrl,
    this.releaseNotes,
  });
}

class UpdateService {
  static const _apiUrl =
      'https://api.github.com/repos/KROOKIMWOOBIN/SoulPal/releases/latest';

  /// 업데이트 가능 여부 확인. 새 버전이 있으면 [UpdateInfo] 반환, 없으면 null.
  static Future<UpdateInfo?> checkForUpdate() async {
    try {
      final info = await PackageInfo.fromPlatform();
      final response = await http
          .get(
            Uri.parse(_apiUrl),
            headers: {'Accept': 'application/vnd.github.v3+json'},
          )
          .timeout(const Duration(seconds: 10));

      if (response.statusCode != 200) return null;

      final data = jsonDecode(response.body) as Map<String, dynamic>;
      final tagName = (data['tag_name'] as String? ?? '').replaceFirst('v', '');
      if (tagName.isEmpty) return null;

      if (!_isNewer(tagName, info.version)) return null;

      final htmlUrl = data['html_url'] as String? ?? _apiUrl;
      final body = data['body'] as String?;

      return UpdateInfo(
        latestVersion: tagName,
        releaseUrl: htmlUrl,
        releaseNotes: body,
      );
    } catch (_) {
      return null;
    }
  }

  /// latest > current 이면 true
  static bool _isNewer(String latest, String current) {
    final a = _parse(latest);
    final b = _parse(current);
    for (int i = 0; i < 3; i++) {
      if (a[i] > b[i]) return true;
      if (a[i] < b[i]) return false;
    }
    return false;
  }

  static List<int> _parse(String v) {
    final parts = v.split('.').map((s) => int.tryParse(s) ?? 0).toList();
    while (parts.length < 3) parts.add(0);
    return parts;
  }
}
