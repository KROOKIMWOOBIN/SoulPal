import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_animate/flutter_animate.dart';
import 'package:provider/provider.dart';
import 'package:share_plus/share_plus.dart';
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
    final characterProvider = context.watch<CharacterProvider>();
    final characters = characterProvider.characters;

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
          // 정렬 버튼
          if (characters.isNotEmpty)
            Semantics(
              label: settings.t('정렬 옵션', 'Sort options'),
              child: PopupMenuButton<CharacterSortOrder>(
                icon: const Icon(Icons.sort_rounded),
                tooltip: settings.t('정렬', 'Sort'),
                shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(16)),
                onSelected: (order) =>
                    characterProvider.setSortOrder(order),
                itemBuilder: (_) => [
                  _sortItem(
                    CharacterSortOrder.recent,
                    Icons.access_time_rounded,
                    settings.t('최근 대화순', 'Recent'),
                    characterProvider.sortOrder,
                  ),
                  _sortItem(
                    CharacterSortOrder.name,
                    Icons.sort_by_alpha_rounded,
                    settings.t('이름순', 'Name'),
                    characterProvider.sortOrder,
                  ),
                  _sortItem(
                    CharacterSortOrder.favorite,
                    Icons.favorite_rounded,
                    settings.t('즐겨찾기 먼저', 'Favorites first'),
                    characterProvider.sortOrder,
                  ),
                ],
              ),
            ),
          Semantics(
            label: settings.t('설정', 'Settings'),
            child: IconButton(
              icon: const Icon(Icons.settings_outlined),
              onPressed: () => Navigator.push(
                context,
                MaterialPageRoute(builder: (_) => const SettingsScreen()),
              ),
            ),
          ),
        ],
      ),
      body: characters.isEmpty
          ? _EmptyState(settings: settings)
          : _CharacterList(characters: characters, settings: settings),
      floatingActionButton: Semantics(
        label: settings.t('새 친구 만들기', 'Create new friend'),
        child: FloatingActionButton.extended(
          onPressed: () => _goToCreation(context),
          icon: const Icon(Icons.add_rounded),
          label: Text(settings.t('친구 만들기', 'Create Friend')),
        ),
      ),
    );
  }

  PopupMenuItem<CharacterSortOrder> _sortItem(
    CharacterSortOrder value,
    IconData icon,
    String label,
    CharacterSortOrder current,
  ) {
    return PopupMenuItem(
      value: value,
      child: Row(
        children: [
          Icon(icon,
              size: 18,
              color: current == value
                  ? const Color(0xFF7C5CBF)
                  : null),
          const SizedBox(width: 8),
          Text(label,
              style: TextStyle(
                  color: current == value
                      ? const Color(0xFF7C5CBF)
                      : null,
                  fontWeight: current == value
                      ? FontWeight.w600
                      : null)),
          if (current == value) ...[
            const Spacer(),
            const Icon(Icons.check_rounded,
                size: 16, color: Color(0xFF7C5CBF)),
          ],
        ],
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
          onLongPress: () => _showCharacterMenu(context, c),
        );
      },
    );
  }

  void _showCharacterMenu(BuildContext context, Character character) {
    showModalBottomSheet(
      context: context,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      builder: (ctx) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const SizedBox(height: 8),
            Container(
              width: 40,
              height: 4,
              decoration: BoxDecoration(
                color: Colors.grey.shade300,
                borderRadius: BorderRadius.circular(2),
              ),
            ),
            const SizedBox(height: 16),
            // 캐릭터 헤더
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 20),
              child: Row(
                children: [
                  Container(
                    width: 48,
                    height: 48,
                    decoration: BoxDecoration(
                      color: character.appearance.color.withOpacity(0.2),
                      shape: BoxShape.circle,
                    ),
                    child: Center(
                      child: Text(character.avatarEmoji,
                          style: const TextStyle(fontSize: 24)),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(character.name,
                          style: const TextStyle(
                              fontWeight: FontWeight.w700, fontSize: 16)),
                      Text(
                        character.relationship.label(settings.locale),
                        style: TextStyle(
                            fontSize: 13,
                            color: character.appearance.color),
                      ),
                    ],
                  ),
                ],
              ),
            ),
            const Divider(height: 24),
            // 메뉴 항목
            _MenuItem(
              icon: character.isFavorite
                  ? Icons.favorite_rounded
                  : Icons.favorite_border_rounded,
              iconColor:
                  character.isFavorite ? Colors.red : null,
              label: character.isFavorite
                  ? settings.t('즐겨찾기 해제', 'Remove favorite')
                  : settings.t('즐겨찾기 추가', 'Add to favorites'),
              onTap: () {
                context
                    .read<CharacterProvider>()
                    .toggleFavorite(character.id);
                Navigator.pop(ctx);
              },
            ),
            _MenuItem(
              icon: Icons.edit_rounded,
              label: settings.t('캐릭터 편집', 'Edit character'),
              onTap: () {
                Navigator.pop(ctx);
                Navigator.push(
                  context,
                  MaterialPageRoute(
                    builder: (_) =>
                        CreationScreen(existing: character),
                  ),
                );
              },
            ),
            _MenuItem(
              icon: Icons.share_rounded,
              label: settings.t('캐릭터 공유', 'Share character'),
              onTap: () {
                Navigator.pop(ctx);
                _shareCharacter(context, character);
              },
            ),
            _MenuItem(
              icon: Icons.copy_rounded,
              label: settings.t('JSON 복사', 'Copy JSON'),
              onTap: () {
                Navigator.pop(ctx);
                _copyCharacterJson(context, character);
              },
            ),
            _MenuItem(
              icon: Icons.delete_outline_rounded,
              iconColor: Colors.red,
              label: settings.t('삭제', 'Delete'),
              labelColor: Colors.red,
              onTap: () {
                Navigator.pop(ctx);
                _showDeleteDialog(context, character);
              },
            ),
            const SizedBox(height: 8),
          ],
        ),
      ),
    );
  }

  void _shareCharacter(BuildContext context, Character character) {
    final json = const JsonEncoder.withIndent('  ')
        .convert(character.toJson());
    final text = settings.t(
      '${character.name} 캐릭터 (SoulPal)\n\n$json',
      '${character.name} character (SoulPal)\n\n$json',
    );
    Share.share(text, subject: 'SoulPal - ${character.name}');
  }

  void _copyCharacterJson(BuildContext context, Character character) {
    final json = const JsonEncoder.withIndent('  ')
        .convert(character.toJson());
    Clipboard.setData(ClipboardData(text: json));
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(settings.t('클립보드에 복사됐어요!', 'Copied to clipboard!')),
        behavior: SnackBarBehavior.floating,
        shape:
            RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      ),
    );
  }

  void _showDeleteDialog(BuildContext context, Character character) {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        shape:
            RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
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
              context
                  .read<CharacterProvider>()
                  .deleteCharacter(character.id);
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

class _MenuItem extends StatelessWidget {
  final IconData icon;
  final Color? iconColor;
  final String label;
  final Color? labelColor;
  final VoidCallback onTap;

  const _MenuItem({
    required this.icon,
    required this.label,
    required this.onTap,
    this.iconColor,
    this.labelColor,
  });

  @override
  Widget build(BuildContext context) {
    return ListTile(
      leading: Icon(icon, color: iconColor),
      title: Text(label,
          style: TextStyle(color: labelColor, fontWeight: FontWeight.w500)),
      onTap: onTap,
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
