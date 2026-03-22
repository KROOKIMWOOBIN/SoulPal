import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../providers/settings_provider.dart';
import '../home/home_screen.dart';

class OnboardingScreen extends StatefulWidget {
  const OnboardingScreen({super.key});

  @override
  State<OnboardingScreen> createState() => _OnboardingScreenState();
}

class _OnboardingScreenState extends State<OnboardingScreen> {
  final _controller = PageController();
  int _page = 0;

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  void _next(SettingsProvider settings) async {
    if (_page < 3) {
      _controller.nextPage(
        duration: const Duration(milliseconds: 350),
        curve: Curves.easeInOut,
      );
    } else {
      await settings.completeOnboarding();
      if (mounted) {
        Navigator.of(context).pushReplacement(
          PageRouteBuilder(
            pageBuilder: (_, __, ___) => const HomeScreen(),
            transitionsBuilder: (_, anim, __, child) =>
                FadeTransition(opacity: anim, child: child),
            transitionDuration: const Duration(milliseconds: 400),
          ),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final settings = context.watch<SettingsProvider>();
    final colorScheme = Theme.of(context).colorScheme;

    final pages = [
      _OnboardingPage(
        emoji: '✨',
        title: settings.t('SoulPal에 오신 걸 환영해요!', 'Welcome to SoulPal!'),
        desc: settings.t(
          '나만의 가상 AI 친구를 만들고\n언제 어디서든 대화해보세요.',
          'Create your own virtual AI friend\nand chat anytime, anywhere.',
        ),
        color: const Color(0xFF7C5CBF),
      ),
      _OnboardingPage(
        emoji: '🤖',
        title: settings.t('완전한 오프라인 AI', 'Fully Offline AI'),
        desc: settings.t(
          'AI가 앱 안에 내장되어 있어요.\n인터넷 없이도 언제든 대화할 수 있고\n모든 대화는 기기에만 저장됩니다.',
          'AI is built right into the app.\nChat anytime without internet —\nall conversations stay on your device.',
        ),
        color: const Color(0xFF4ECDC4),
      ),
      _OnboardingPage(
        emoji: '🎨',
        title: settings.t('나만의 캐릭터 제작', 'Craft Your Character'),
        desc: settings.t(
          '관계, 성격, 말투, 관심사, 외모까지\n6가지 카테고리로 완전히 맞춤 설정해\n특별한 친구를 만들어보세요.',
          'Customize 6 categories: relationship,\npersonality, speech style, interests & vibe\nto create a truly unique friend.',
        ),
        color: const Color(0xFFFF85A1),
      ),
      _OnboardingPage(
        emoji: '💬',
        title: settings.t('지금 바로 시작해요!', 'Let\'s Get Started!'),
        desc: settings.t(
          '첫 번째 AI 친구를 만들어볼까요?\n친구 목록에서 + 버튼을 눌러\n새로운 친구를 만들 수 있어요.',
          'Ready to meet your first AI friend?\nTap the + button on the home screen\nto create your companion!',
        ),
        color: const Color(0xFFFF9F43),
      ),
    ];

    return Scaffold(
      backgroundColor: pages[_page].color,
      body: SafeArea(
        child: Column(
          children: [
            // Skip 버튼
            Align(
              alignment: Alignment.topRight,
              child: Padding(
                padding: const EdgeInsets.only(top: 8, right: 8),
                child: TextButton(
                  onPressed: () async {
                    await settings.completeOnboarding();
                    if (mounted) {
                      Navigator.of(context).pushReplacement(
                        PageRouteBuilder(
                          pageBuilder: (_, __, ___) => const HomeScreen(),
                          transitionsBuilder: (_, anim, __, child) =>
                              FadeTransition(opacity: anim, child: child),
                        ),
                      );
                    }
                  },
                  child: Text(
                    settings.t('건너뛰기', 'Skip'),
                    style: const TextStyle(color: Colors.white70, fontSize: 14),
                  ),
                ),
              ),
            ),

            // 페이지 내용
            Expanded(
              child: PageView.builder(
                controller: _controller,
                onPageChanged: (i) => setState(() => _page = i),
                itemCount: pages.length,
                itemBuilder: (_, i) => pages[i],
              ),
            ),

            // 하단 점 인디케이터 + 버튼
            Padding(
              padding: const EdgeInsets.fromLTRB(24, 0, 24, 40),
              child: Column(
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: List.generate(
                      pages.length,
                      (i) => AnimatedContainer(
                        duration: const Duration(milliseconds: 300),
                        margin: const EdgeInsets.symmetric(horizontal: 4),
                        width: _page == i ? 24 : 8,
                        height: 8,
                        decoration: BoxDecoration(
                          color: _page == i
                              ? Colors.white
                              : Colors.white38,
                          borderRadius: BorderRadius.circular(4),
                        ),
                      ),
                    ),
                  ),
                  const SizedBox(height: 24),
                  SizedBox(
                    width: double.infinity,
                    child: ElevatedButton(
                      onPressed: () => _next(settings),
                      style: ElevatedButton.styleFrom(
                        backgroundColor: Colors.white,
                        foregroundColor: pages[_page].color,
                        padding: const EdgeInsets.symmetric(vertical: 16),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(16),
                        ),
                        textStyle: const TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                      child: Text(
                        _page < 3
                            ? settings.t('다음', 'Next')
                            : settings.t('시작하기!', 'Get Started!'),
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _OnboardingPage extends StatelessWidget {
  final String emoji;
  final String title;
  final String desc;
  final Color color;

  const _OnboardingPage({
    required this.emoji,
    required this.title,
    required this.desc,
    required this.color,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 32),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Text(emoji, style: const TextStyle(fontSize: 88)),
          const SizedBox(height: 32),
          Text(
            title,
            style: const TextStyle(
              fontSize: 26,
              fontWeight: FontWeight.w800,
              color: Colors.white,
              height: 1.3,
            ),
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: 16),
          Text(
            desc,
            style: TextStyle(
              fontSize: 15,
              color: Colors.white.withOpacity(0.8),
              height: 1.7,
            ),
            textAlign: TextAlign.center,
          ),
        ],
      ),
    );
  }
}
