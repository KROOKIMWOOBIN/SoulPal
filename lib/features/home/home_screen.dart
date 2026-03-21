import 'package:flutter/material.dart';
import 'package:flutter_animate/flutter_animate.dart';
import 'package:provider/provider.dart';
import '../../models/character.dart';
import '../../providers/character_provider.dart';
import '../../providers/settings_provider.dart';
import '../chat/chat_screen.dart';
import '../creation/creation_screen.dart';
import '../settings/settings_screen.dart';
import 'widgets/character_card.dart';

class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final settings = context.watch<SettingsProvider>();
    final characters = context.watch<CharacterProvider>().characters;

    return Scaffold(
      appBar: AppBar(
        title: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(
              '✨ SoulPal',
              style: Theme.of(context).textTheme.titleLarge?.copyWith(
                    fontWeight: FontWeight.w800,
                    color: const Color(0xFF7C5CBF),
                  ),
            ),
          ],
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.settings_outlined),
            onPressed: () => Navigator.push(
              context,
              MaterialPageRoute(builder: (_) => const SettingsScreen()),
            ),
          ),
        ],
      ),
      body: characters.isEmpty
          ? _EmptyState(settings: settings)
          : _CharacterList(characters: characters, settings: settings),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () => _goToCreation(context),
        icon: const Icon(Icons.add_rounded),
        label: Text(settings.t('친구 만들기', 'Create Friend')),
      ),
    );
  }

  void _goToCreation(BuildContext context) {
    Navigator.push(
      context,
      MaterialPageRoute(builder: (_) => const CreationScreen()),
    );
  }
}

class _CharacterList extends StatelessWidget {
  final List<Character> characters;
  final SettingsProvider settings;

  const _CharacterList(
      {required this.characters, required this.settings});

  @override
  Widget build(BuildContext context) {
    return ListView.builder(
      padding: const EdgeInsets.only(top: 8, bottom: 100),
      itemCount: characters.length,
      itemBuilder: (ctx, i) {
        final c = characters[i];
        return CharacterCard(
          character: c,
          locale: settings.locale,
          onTap: () => Navigator.push(
            context,
            MaterialPageRoute(builder: (_) => ChatScreen(character: c)),
          ),
          onLongPress: () => _showDeleteDialog(context, c),
        );
      },
    );
  }

  void _showDeleteDialog(BuildContext context, Character character) {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        title: Text(settings.t('캐릭터 삭제', 'Delete Character')),
        content: Text(settings.t(
          '${character.name}을(를) 삭제할까요?\n대화 기록도 함께 삭제됩니다.',
          'Delete ${character.name}?\nChat history will also be deleted.',
        )),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: Text(settings.t('취소', 'Cancel')),
          ),
          TextButton(
            onPressed: () {
              context.read<CharacterProvider>().deleteCharacter(character.id);
              Navigator.pop(ctx);
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

class _EmptyState extends StatelessWidget {
  final SettingsProvider settings;
  const _EmptyState({required this.settings});

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          const Text('🌟', style: TextStyle(fontSize: 72))
              .animate()
              .scale(duration: 600.ms, curve: Curves.elasticOut),
          const SizedBox(height: 20),
          Text(
            settings.t('첫 번째 친구를 만들어보세요!', 'Create your first friend!'),
            style: Theme.of(context).textTheme.titleLarge?.copyWith(
                  color: const Color(0xFF7C5CBF),
                ),
          ).animate().fadeIn(delay: 200.ms),
          const SizedBox(height: 8),
          Text(
            settings.t(
              '카테고리를 선택해 나만의 가상 친구를 만들어요.',
              'Select categories to create your virtual friend.',
            ),
            style: Theme.of(context).textTheme.bodyMedium,
            textAlign: TextAlign.center,
          ).animate().fadeIn(delay: 300.ms),
        ],
      ),
    );
  }
}
