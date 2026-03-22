import 'package:flutter/material.dart';
import 'package:package_info_plus/package_info_plus.dart';
import 'package:provider/provider.dart';
import '../../providers/chat_provider.dart';
import '../../providers/settings_provider.dart';
import '../../services/model_manager.dart';

class SettingsScreen extends StatelessWidget {
  const SettingsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final settings = context.watch<SettingsProvider>();
    final chat = context.watch<ChatProvider>();
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return Scaffold(
      appBar: AppBar(
        title: Text(settings.t('설정', 'Settings')),
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // ── AI 상태 ────────────────────────────────────────────
          _SectionTitle(label: settings.t('AI 상태', 'AI Status')),
          _Card(
            child: Row(
              children: [
                Container(
                  width: 12,
                  height: 12,
                  decoration: BoxDecoration(
                    color: chat.isLlmReady ? Colors.green : Colors.orange,
                    shape: BoxShape.circle,
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: Text(
                    chat.isLlmReady
                        ? settings.t(
                            'AI 준비 완료 (Llama 3.2 1B)',
                            'AI Ready (Llama 3.2 1B)',
                          )
                        : settings.t(
                            'AI 초기화 중...',
                            'Initializing AI...',
                          ),
                    style: const TextStyle(fontWeight: FontWeight.w600),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),

          // ── 테마 ───────────────────────────────────────────────
          _SectionTitle(label: settings.t('테마', 'Theme')),
          _Card(
            child: Column(
              children: [
                _ThemeTile(
                  icon: Icons.light_mode_rounded,
                  label: settings.t('라이트 모드', 'Light'),
                  value: ThemeMode.light,
                  groupValue: settings.themeMode,
                  onChanged: (v) => settings.setThemeMode(v!),
                ),
                _ThemeTile(
                  icon: Icons.dark_mode_rounded,
                  label: settings.t('다크 모드', 'Dark'),
                  value: ThemeMode.dark,
                  groupValue: settings.themeMode,
                  onChanged: (v) => settings.setThemeMode(v!),
                ),
                _ThemeTile(
                  icon: Icons.brightness_auto_rounded,
                  label: settings.t('시스템 설정', 'System'),
                  value: ThemeMode.system,
                  groupValue: settings.themeMode,
                  onChanged: (v) => settings.setThemeMode(v!),
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),

          // ── 언어 ───────────────────────────────────────────────
          _SectionTitle(label: settings.t('언어', 'Language')),
          _Card(
            child: Column(
              children: [
                RadioListTile<String>(
                  title: const Text('한국어'),
                  value: 'ko',
                  groupValue: settings.locale,
                  activeColor: const Color(0xFF7C5CBF),
                  onChanged: (v) => settings.setLocale(v!),
                ),
                RadioListTile<String>(
                  title: const Text('English'),
                  value: 'en',
                  groupValue: settings.locale,
                  activeColor: const Color(0xFF7C5CBF),
                  onChanged: (v) => settings.setLocale(v!),
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),

          // ── AI 파라미터 ───────────────────────────────────────
          _SectionTitle(label: settings.t('AI 파라미터', 'AI Parameters')),
          _Card(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // Temperature
                _ParamRow(
                  label: settings.t('창의성 (Temperature)', 'Creativity (Temperature)'),
                  value: settings.temperature.toStringAsFixed(1),
                  hint: settings.t(
                    '낮을수록 일관된 답변, 높을수록 창의적인 답변',
                    'Lower = consistent, Higher = creative',
                  ),
                ),
                Slider(
                  value: settings.temperature,
                  min: 0.1,
                  max: 2.0,
                  divisions: 19,
                  activeColor: const Color(0xFF7C5CBF),
                  onChanged: (v) => settings.setTemperature(v),
                ),
                const SizedBox(height: 8),

                // History Count
                _ParamRow(
                  label: settings.t('대화 기억 개수', 'Conversation memory'),
                  value: settings.t(
                    '${settings.historyCount}개',
                    '${settings.historyCount} msgs',
                  ),
                  hint: settings.t(
                    'AI가 참고하는 이전 메시지 수',
                    'Number of previous messages AI references',
                  ),
                ),
                Slider(
                  value: settings.historyCount.toDouble(),
                  min: 1,
                  max: 30,
                  divisions: 29,
                  activeColor: const Color(0xFF7C5CBF),
                  onChanged: (v) =>
                      settings.setHistoryCount(v.round()),
                ),
                const SizedBox(height: 8),

                // Context Length
                _ParamRow(
                  label: settings.t('컨텍스트 길이', 'Context Length'),
                  value: '${settings.contextLength}',
                  hint: settings.t(
                    '높을수록 더 긴 대화 가능 (메모리 사용 증가)',
                    'Higher = longer context (more memory)',
                  ),
                ),
                DropdownButtonHideUnderline(
                  child: DropdownButton<int>(
                    value: settings.contextLength,
                    isExpanded: true,
                    borderRadius: BorderRadius.circular(12),
                    items: const [512, 1024, 2048, 4096].map((v) {
                      return DropdownMenuItem(
                        value: v,
                        child: Text('$v tokens'),
                      );
                    }).toList(),
                    onChanged: (v) =>
                        settings.setContextLength(v!),
                  ),
                ),
                const SizedBox(height: 8),

                Align(
                  alignment: Alignment.centerRight,
                  child: TextButton.icon(
                    onPressed: () => settings.resetLlmParams(),
                    icon: const Icon(Icons.refresh_rounded, size: 16),
                    label: Text(settings.t('기본값으로 재설정', 'Reset to defaults')),
                    style: TextButton.styleFrom(
                      foregroundColor: const Color(0xFF7B6F8A),
                    ),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 8),
          _Card(
            color: isDark ? const Color(0xFF2A2510) : const Color(0xFFFFF8E7),
            child: Row(
              children: [
                const Text('⚠️', style: TextStyle(fontSize: 16)),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    settings.t(
                      'AI 파라미터 변경은 다음 앱 재시작 시 적용됩니다.',
                      'AI parameter changes apply on next app restart.',
                    ),
                    style: TextStyle(
                        fontSize: 12,
                        color: isDark
                            ? const Color(0xFFAA9EC4)
                            : const Color(0xFF7B6F8A)),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),

          // ── 모델 관리 ──────────────────────────────────────────
          _SectionTitle(label: settings.t('모델 관리', 'Model Management')),
          _Card(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    const Text('🤖', style: TextStyle(fontSize: 20)),
                    const SizedBox(width: 10),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Text(
                            'Llama 3.2 1B Instruct Q4_K_M',
                            style:
                                TextStyle(fontWeight: FontWeight.w700),
                          ),
                          Text(
                            settings.t(
                              '약 800MB • HuggingFace (bartowski)',
                              '~800MB • HuggingFace (bartowski)',
                            ),
                            style: const TextStyle(
                              fontSize: 12,
                              color: Color(0xFF7B6F8A),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 14),
                OutlinedButton.icon(
                  onPressed: () => _confirmDeleteModel(context, settings),
                  style: OutlinedButton.styleFrom(
                    foregroundColor: Colors.red,
                    side: const BorderSide(color: Colors.red),
                  ),
                  icon: const Icon(Icons.delete_outline, size: 18),
                  label: Text(
                      settings.t('모델 삭제 및 재다운로드', 'Delete & Re-download')),
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),

          // ── 안내 ───────────────────────────────────────────────
          _Card(
            color: isDark ? const Color(0xFF1E1A2E) : const Color(0xFFF5F0FF),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    const Text('💡', style: TextStyle(fontSize: 16)),
                    const SizedBox(width: 6),
                    Text(
                      settings.t('SoulPal 특징', 'About SoulPal'),
                      style:
                          const TextStyle(fontWeight: FontWeight.w700),
                    ),
                  ],
                ),
                const SizedBox(height: 8),
                _Tip(text: settings.t(
                  'AI가 앱 안에 내장되어 인터넷 없이도 동작합니다',
                  'AI is embedded in the app — works offline',
                )),
                _Tip(text: settings.t(
                  '대화 내용은 기기에만 저장되어 외부로 전송되지 않습니다',
                  'All conversations stay on your device — never uploaded',
                )),
                _Tip(text: settings.t(
                  '첫 실행 시 AI 모델을 한 번만 다운로드합니다 (~800MB)',
                  'AI model is downloaded once on first launch (~800MB)',
                )),
              ],
            ),
          ),
          const SizedBox(height: 32),

          // ── About ──────────────────────────────────────────────
          Center(
            child: Column(
              children: [
                const Text('✨ SoulPal',
                    style: TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.w800,
                      color: Color(0xFF7C5CBF),
                    )),
                const SizedBox(height: 4),
                Text(
                  settings.t(
                    '로컬 AI로 만드는 나만의 가상 친구',
                    'Your virtual friend powered by on-device AI',
                  ),
                  style: const TextStyle(
                      fontSize: 12, color: Color(0xFF7B6F8A)),
                ),
                const SizedBox(height: 4),
                FutureBuilder<PackageInfo>(
                  future: PackageInfo.fromPlatform(),
                  builder: (ctx, snap) {
                    final version = snap.hasData
                        ? 'v${snap.data!.version}+${snap.data!.buildNumber}'
                        : 'v—';
                    return Text(version,
                        style: const TextStyle(
                            fontSize: 12, color: Color(0xFFB0A8C8)));
                  },
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),
        ],
      ),
    );
  }

  void _confirmDeleteModel(
      BuildContext context, SettingsProvider settings) {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(20)),
        title: Text(settings.t('모델 삭제', 'Delete Model')),
        content: Text(settings.t(
          '모델 파일을 삭제하면 앱 재시작 시 다시 다운로드됩니다.',
          'The model will be re-downloaded on next app restart.',
        )),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: Text(settings.t('취소', 'Cancel')),
          ),
          TextButton(
            onPressed: () async {
              await ModelManager.delete();
              if (ctx.mounted) Navigator.pop(ctx);
              if (context.mounted) {
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(
                    content: Text(settings.t(
                      '삭제됨. 앱을 재시작하면 다시 다운로드됩니다.',
                      'Deleted. Re-download on next restart.',
                    )),
                    behavior: SnackBarBehavior.floating,
                  ),
                );
              }
            },
            child: Text(
              settings.t('삭제', 'Delete'),
              style: const TextStyle(color: Colors.red),
            ),
          ),
        ],
      ),
    );
  }
}

// ─── 파라미터 행 ─────────────────────────────────────────────────────────────
class _ParamRow extends StatelessWidget {
  final String label;
  final String value;
  final String hint;

  const _ParamRow({
    required this.label,
    required this.value,
    required this.hint,
  });

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text(label,
                style: const TextStyle(
                    fontWeight: FontWeight.w600, fontSize: 14)),
            Container(
              padding:
                  const EdgeInsets.symmetric(horizontal: 10, vertical: 3),
              decoration: BoxDecoration(
                color: const Color(0xFF7C5CBF).withOpacity(0.12),
                borderRadius: BorderRadius.circular(8),
              ),
              child: Text(
                value,
                style: const TextStyle(
                  color: Color(0xFF7C5CBF),
                  fontWeight: FontWeight.w700,
                  fontSize: 13,
                ),
              ),
            ),
          ],
        ),
        Text(hint,
            style: TextStyle(
                fontSize: 12,
                color: isDark ? const Color(0xFFAA9EC4) : const Color(0xFF7B6F8A))),
      ],
    );
  }
}

// ─── 테마 타일 ────────────────────────────────────────────────────────────────
class _ThemeTile extends StatelessWidget {
  final IconData icon;
  final String label;
  final ThemeMode value;
  final ThemeMode groupValue;
  final ValueChanged<ThemeMode?> onChanged;

  const _ThemeTile({
    required this.icon,
    required this.label,
    required this.value,
    required this.groupValue,
    required this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    return RadioListTile<ThemeMode>(
      value: value,
      groupValue: groupValue,
      activeColor: const Color(0xFF7C5CBF),
      onChanged: onChanged,
      title: Row(
        children: [
          Icon(icon,
              size: 18,
              color: Theme.of(context).brightness == Brightness.dark
                  ? const Color(0xFFAA9EC4)
                  : const Color(0xFF7B6F8A)),
          const SizedBox(width: 8),
          Text(label),
        ],
      ),
    );
  }
}

// ─── 공통 위젯 ────────────────────────────────────────────────────────────────
class _SectionTitle extends StatelessWidget {
  final String label;
  const _SectionTitle({required this.label});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8, left: 4),
      child: Text(
        label,
        style: const TextStyle(
          fontSize: 13,
          fontWeight: FontWeight.w700,
          color: Color(0xFF7C5CBF),
          letterSpacing: 0.5,
        ),
      ),
    );
  }
}

class _Card extends StatelessWidget {
  final Widget child;
  final Color? color;
  const _Card({required this.child, this.color});

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final defaultColor = isDark ? const Color(0xFF2E2544) : Colors.white;

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: color ?? defaultColor,
        borderRadius: BorderRadius.circular(16),
        boxShadow: color == null
            ? [
                BoxShadow(
                  color: Colors.black.withOpacity(0.05),
                  blurRadius: 8,
                  offset: const Offset(0, 2),
                )
              ]
            : [],
      ),
      child: child,
    );
  }
}

class _Tip extends StatelessWidget {
  final String text;
  const _Tip({required this.text});

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    return Padding(
      padding: const EdgeInsets.only(top: 4),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('• ',
              style: TextStyle(color: Color(0xFF7C5CBF))),
          Expanded(
            child: Text(
              text,
              style: TextStyle(
                  fontSize: 13,
                  color: isDark ? const Color(0xFFEDE8FF) : const Color(0xFF2D2040)),
            ),
          ),
        ],
      ),
    );
  }
}
