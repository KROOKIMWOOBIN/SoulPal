import 'package:flutter/material.dart';
import 'package:flutter_animate/flutter_animate.dart';
import 'package:provider/provider.dart';
import '../../providers/settings_provider.dart';
import '../../providers/chat_provider.dart';
import '../../services/model_manager.dart';
import '../home/home_screen.dart';

enum _Phase {
  checking,    // 모델 파일 확인 중
  downloading, // 다운로드 중
  loading,     // llama.cpp 모델 로드 중
  ready,       // 완료
  error,       // 오류
}

class SplashScreen extends StatefulWidget {
  const SplashScreen({super.key});

  @override
  State<SplashScreen> createState() => _SplashScreenState();
}

class _SplashScreenState extends State<SplashScreen> {
  _Phase _phase = _Phase.checking;
  double? _downloadPercent;
  String _statusText = '';
  String _downloadedStr = '';
  String _totalStr = '';
  String? _errorMessage;

  @override
  void initState() {
    super.initState();
    Future.delayed(const Duration(milliseconds: 600), _start);
  }

  String _t(String ko, String en) =>
      context.read<SettingsProvider>().isKorean ? ko : en;

  Future<void> _start() async {
    // 1. 모델 파일 존재 확인
    setState(() {
      _phase = _Phase.checking;
      _statusText = _t('모델 확인 중...', 'Checking model...');
    });

    final ready = await ModelManager.isReady();

    if (ready) {
      await _loadModel();
    } else {
      await _download();
    }
  }

  Future<void> _download() async {
    setState(() {
      _phase = _Phase.downloading;
      _downloadPercent = null;
      _statusText = _t('AI 모델 다운로드 중...', 'Downloading AI model...');
    });

    try {
      await for (final p in ModelManager.download()) {
        if (!mounted) return;
        setState(() {
          _downloadPercent = p.percent;
          _downloadedStr = p.receivedStr;
          _totalStr = p.totalStr;
          if (p.percent != null) {
            _statusText = _t(
              '다운로드 중... ${(p.percent! * 100).toStringAsFixed(1)}%',
              'Downloading... ${(p.percent! * 100).toStringAsFixed(1)}%',
            );
          }
        });
      }

      // 다운로드 완료 후 모델 로드
      await _loadModel();
    } catch (e) {
      if (mounted) {
        setState(() {
          _phase = _Phase.error;
          _errorMessage = e.toString();
        });
      }
    }
  }

  Future<void> _loadModel() async {
    setState(() {
      _phase = _Phase.loading;
      _statusText = _t('AI 초기화 중...', 'Initializing AI...');
    });

    try {
      final modelPath = await ModelManager.modelPath;
      await context.read<ChatProvider>().initializeLlm(modelPath);

      if (mounted) _goHome();
    } catch (e) {
      if (mounted) {
        setState(() {
          _phase = _Phase.error;
          _errorMessage = _t(
            '모델 로드 실패: $e\n\n모델 파일을 삭제하고 다시 다운로드합니다.',
            'Failed to load model: $e\n\nWill re-download model file.',
          );
        });
      }
    }
  }

  void _goHome() {
    Navigator.of(context).pushReplacement(
      PageRouteBuilder(
        pageBuilder: (_, __, ___) => const HomeScreen(),
        transitionsBuilder: (_, anim, __, child) =>
            FadeTransition(opacity: anim, child: child),
        transitionDuration: const Duration(milliseconds: 500),
      ),
    );
  }

  Future<void> _retryAfterDelete() async {
    await ModelManager.delete();
    _start();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF7C5CBF),
      body: SafeArea(
        child: Column(
          children: [
            // ── 로고 ──────────────────────────────────────────
            const Expanded(child: _Logo()),

            // ── 상태/진행률 ───────────────────────────────────
            Padding(
              padding: const EdgeInsets.fromLTRB(32, 0, 32, 52),
              child: _buildContent(),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildContent() {
    switch (_phase) {
      case _Phase.checking:
      case _Phase.loading:
        return _SpinnerRow(text: _statusText);

      case _Phase.downloading:
        return _DownloadView(
          percent: _downloadPercent,
          statusText: _statusText,
          downloadedStr: _downloadedStr,
          totalStr: _totalStr,
          note: _t(
            '첫 실행 시 AI 모델을 다운로드합니다 (~800MB)\n이후에는 오프라인으로 동작합니다.',
            'Downloading AI model on first launch (~800MB)\nWorks offline afterwards.',
          ),
        );

      case _Phase.error:
        return _ErrorView(
          message: _errorMessage ?? '',
          onRetry: _start,
          onRedownload: _retryAfterDelete,
          settings: context.watch<SettingsProvider>(),
        );

      case _Phase.ready:
        return const SizedBox.shrink();
    }
  }
}

// ─── 하위 위젯 ────────────────────────────────────────────────────────────────

class _Logo extends StatelessWidget {
  const _Logo();

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Text('✨', style: TextStyle(fontSize: 72))
              .animate()
              .scale(duration: 700.ms, curve: Curves.elasticOut),
          const SizedBox(height: 16),
          const Text(
            'SoulPal',
            style: TextStyle(
              fontSize: 42,
              fontWeight: FontWeight.w900,
              color: Colors.white,
              letterSpacing: 1,
            ),
          ).animate().fadeIn(delay: 300.ms),
          const SizedBox(height: 8),
          const Text(
            '나만의 가상 친구',
            style: TextStyle(fontSize: 16, color: Colors.white70),
          ).animate().fadeIn(delay: 500.ms),
        ],
      ),
    );
  }
}

class _SpinnerRow extends StatelessWidget {
  final String text;
  const _SpinnerRow({required this.text});

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        const SizedBox(
          width: 18,
          height: 18,
          child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white70),
        ),
        const SizedBox(width: 10),
        Text(text, style: const TextStyle(color: Colors.white70, fontSize: 14)),
      ],
    );
  }
}

class _DownloadView extends StatelessWidget {
  final double? percent;
  final String statusText;
  final String downloadedStr;
  final String totalStr;
  final String note;

  const _DownloadView({
    required this.percent,
    required this.statusText,
    required this.downloadedStr,
    required this.totalStr,
    required this.note,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        // 퍼센트 숫자
        if (percent != null)
          Text(
            '${(percent! * 100).toStringAsFixed(1)}%',
            style: const TextStyle(
              color: Colors.white,
              fontSize: 28,
              fontWeight: FontWeight.w700,
            ),
          )
              .animate(onPlay: (c) => c.repeat())
              .shimmer(color: Colors.white38, duration: 1800.ms)
        else
          const SizedBox(
            width: 22,
            height: 22,
            child: CircularProgressIndicator(strokeWidth: 2.5, color: Colors.white70),
          ),

        const SizedBox(height: 12),

        // 프로그레스 바
        ClipRRect(
          borderRadius: BorderRadius.circular(8),
          child: LinearProgressIndicator(
            value: percent,
            backgroundColor: Colors.white24,
            valueColor: const AlwaysStoppedAnimation<Color>(Colors.white),
            minHeight: 10,
          ),
        ),

        const SizedBox(height: 8),

        // 받은 용량 / 전체
        if (downloadedStr.isNotEmpty && totalStr.isNotEmpty)
          Text(
            '$downloadedStr / $totalStr',
            style: const TextStyle(color: Colors.white60, fontSize: 12),
          ),

        const SizedBox(height: 6),
        Text(
          statusText,
          style: const TextStyle(color: Colors.white70, fontSize: 13),
          textAlign: TextAlign.center,
        ),

        const SizedBox(height: 10),
        Text(
          note,
          style: const TextStyle(color: Colors.white38, fontSize: 11),
          textAlign: TextAlign.center,
        ),
      ],
    );
  }
}

class _ErrorView extends StatelessWidget {
  final String message;
  final VoidCallback onRetry;
  final VoidCallback onRedownload;
  final SettingsProvider settings;

  const _ErrorView({
    required this.message,
    required this.onRetry,
    required this.onRedownload,
    required this.settings,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        const Icon(Icons.error_outline_rounded,
            color: Colors.orangeAccent, size: 36),
        const SizedBox(height: 12),
        Text(
          settings.t('오류가 발생했습니다', 'An error occurred'),
          style: const TextStyle(
              color: Colors.white, fontWeight: FontWeight.w700, fontSize: 16),
        ),
        const SizedBox(height: 6),
        Text(
          message,
          style: const TextStyle(color: Colors.white60, fontSize: 12),
          textAlign: TextAlign.center,
          maxLines: 4,
          overflow: TextOverflow.ellipsis,
        ),
        const SizedBox(height: 20),
        Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            ElevatedButton.icon(
              onPressed: onRetry,
              style: ElevatedButton.styleFrom(
                backgroundColor: Colors.white,
                foregroundColor: const Color(0xFF7C5CBF),
              ),
              icon: const Icon(Icons.refresh_rounded),
              label: Text(settings.t('다시 시도', 'Retry')),
            ),
            const SizedBox(width: 12),
            OutlinedButton.icon(
              onPressed: onRedownload,
              style: OutlinedButton.styleFrom(
                foregroundColor: Colors.white70,
                side: const BorderSide(color: Colors.white38),
              ),
              icon: const Icon(Icons.download_rounded, size: 18),
              label: Text(settings.t('재다운로드', 'Re-download')),
            ),
          ],
        ),
      ],
    );
  }
}
