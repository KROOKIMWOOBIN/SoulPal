import 'package:flutter/material.dart';
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
                            style: TextStyle(fontWeight: FontWeight.w700),
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
                  label: Text(settings.t('모델 삭제 및 재다운로드', 'Delete & Re-download')),
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),

          // ── 안내 ───────────────────────────────────────────────
          _Card(
            color: const Color(0xFFF5F0FF),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    const Text('💡', style: TextStyle(fontSize: 16)),
                    const SizedBox(width: 6),
                    Text(
                      settings.t('SoulPal 특징', 'About SoulPal'),
                      style: const TextStyle(fontWeight: FontWeight.w700),
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
                  style: const TextStyle(fontSize: 12, color: Color(0xFF7B6F8A)),
                ),
                const SizedBox(height: 4),
                const Text('v1.0.0',
                    style: TextStyle(fontSize: 12, color: Color(0xFFB0A8C8))),
              ],
            ),
          ),
        ],
      ),
    );
  }

  void _confirmDeleteModel(BuildContext context, SettingsProvider settings) {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
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
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: color ?? Colors.white,
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
    return Padding(
      padding: const EdgeInsets.only(top: 4),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('• ', style: TextStyle(color: Color(0xFF7C5CBF))),
          Expanded(
            child: Text(
              text,
              style: const TextStyle(fontSize: 13, color: Color(0xFF2D2040)),
            ),
          ),
        ],
      ),
    );
  }
}
