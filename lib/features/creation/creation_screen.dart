import 'package:flutter/material.dart';
import 'package:flutter_animate/flutter_animate.dart';
import 'package:provider/provider.dart';
import 'package:uuid/uuid.dart';
import '../../core/constants/categories.dart';
import '../../models/character.dart';
import '../../providers/character_provider.dart';
import '../../providers/settings_provider.dart';
import 'widgets/category_option_card.dart';
import 'widgets/step_indicator.dart';

class CreationScreen extends StatefulWidget {
  const CreationScreen({super.key});

  @override
  State<CreationScreen> createState() => _CreationScreenState();
}

class _CreationScreenState extends State<CreationScreen> {
  int _step = 0;
  static const int _totalSteps = 6;

  // Selections
  String? _relationshipId;
  String? _personalityId;
  String? _speechStyleId;
  final Set<String> _interestIds = {};
  String? _appearanceId;
  final _nameController = TextEditingController();
  bool _nameEdited = false;

  static const _uuid = Uuid();

  @override
  void dispose() {
    _nameController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final settings = context.watch<SettingsProvider>();

    return Scaffold(
      appBar: AppBar(
        title: Text(settings.t('캐릭터 만들기', 'Create Character')),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_ios_rounded),
          onPressed: () {
            if (_step == 0) {
              Navigator.pop(context);
            } else {
              setState(() => _step--);
            }
          },
        ),
      ),
      body: Column(
        children: [
          const SizedBox(height: 8),
          StepIndicator(currentStep: _step, totalSteps: _totalSteps),
          const SizedBox(height: 20),
          Expanded(
            child: AnimatedSwitcher(
              duration: const Duration(milliseconds: 300),
              transitionBuilder: (child, animation) => FadeTransition(
                opacity: animation,
                child: SlideTransition(
                  position: Tween<Offset>(
                    begin: const Offset(0.05, 0),
                    end: Offset.zero,
                  ).animate(animation),
                  child: child,
                ),
              ),
              child: _buildStep(settings),
            ),
          ),
          _buildNavButtons(context, settings),
        ],
      ),
    );
  }

  Widget _buildStep(SettingsProvider settings) {
    switch (_step) {
      case 0:
        return _CategoryStep(
          key: const ValueKey('relationship'),
          title: settings.t('어떤 관계의 친구인가요?', 'What kind of friend?'),
          subtitle: settings.t('관계 유형을 선택해주세요', 'Select relationship type'),
          items: CategoryData.relationships,
          selected: _relationshipId != null ? {_relationshipId!} : {},
          locale: settings.locale,
          multiSelect: false,
          onToggle: (id) => setState(() => _relationshipId = id),
        );
      case 1:
        return _CategoryStep(
          key: const ValueKey('personality'),
          title: settings.t('어떤 성격인가요?', 'What personality?'),
          subtitle: settings.t('성격을 선택해주세요', 'Select personality'),
          items: CategoryData.personalities,
          selected: _personalityId != null ? {_personalityId!} : {},
          locale: settings.locale,
          multiSelect: false,
          onToggle: (id) => setState(() => _personalityId = id),
        );
      case 2:
        return _CategoryStep(
          key: const ValueKey('speech'),
          title: settings.t('어떤 말투인가요?', 'What speech style?'),
          subtitle: settings.t('말투를 선택해주세요', 'Select speech style'),
          items: CategoryData.speechStyles,
          selected: _speechStyleId != null ? {_speechStyleId!} : {},
          locale: settings.locale,
          multiSelect: false,
          onToggle: (id) => setState(() => _speechStyleId = id),
        );
      case 3:
        return _CategoryStep(
          key: const ValueKey('interests'),
          title: settings.t('어떤 것에 관심 있나요?', 'What are their interests?'),
          subtitle: settings.t('여러 개 선택 가능해요', 'Multiple selection allowed'),
          items: CategoryData.interests,
          selected: _interestIds,
          locale: settings.locale,
          multiSelect: true,
          onToggle: (id) => setState(() {
            if (_interestIds.contains(id)) {
              _interestIds.remove(id);
            } else {
              _interestIds.add(id);
            }
          }),
        );
      case 4:
        return _CategoryStep(
          key: const ValueKey('appearance'),
          title: settings.t('어떤 분위기인가요?', 'What is their vibe?'),
          subtitle: settings.t('외모/분위기를 선택해주세요', 'Select appearance style'),
          items: CategoryData.appearances,
          selected: _appearanceId != null ? {_appearanceId!} : {},
          locale: settings.locale,
          multiSelect: false,
          onToggle: (id) => setState(() => _appearanceId = id),
        );
      case 5:
        return _NameStep(
          key: const ValueKey('name'),
          controller: _nameController,
          suggestedName: _getSuggestedName(),
          settings: settings,
          onNameChanged: () => setState(() => _nameEdited = true),
        );
      default:
        return const SizedBox.shrink();
    }
  }

  String _getSuggestedName() {
    final appearance = _appearanceId != null
        ? CategoryData.getAppearance(_appearanceId!)
        : null;
    final names = {
      'cute': ['미아', 'Mia', '나나'],
      'cool': ['루나', 'Luna', '재이'],
      'warm': ['소피', 'Sophie', '하늘'],
      'sporty': ['레이', 'Ray', '준혁'],
      'intellectual': ['아이린', 'Irene', '지현'],
      'natural': ['루비', 'Ruby', '솔'],
    };
    final list = names[appearance?.id] ?? ['솔', 'Sol', '하루'];
    return list[DateTime.now().millisecond % list.length];
  }

  bool _canProceed() {
    switch (_step) {
      case 0: return _relationshipId != null;
      case 1: return _personalityId != null;
      case 2: return _speechStyleId != null;
      case 3: return _interestIds.isNotEmpty;
      case 4: return _appearanceId != null;
      case 5: return _nameController.text.trim().isNotEmpty;
      default: return false;
    }
  }

  Widget _buildNavButtons(BuildContext context, SettingsProvider settings) {
    final isLast = _step == _totalSteps - 1;
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 0, 16, 32),
      child: SizedBox(
        width: double.infinity,
        child: ElevatedButton(
          onPressed: _canProceed() ? () => _onNext(context) : null,
          child: Text(
            isLast
                ? settings.t('캐릭터 생성!', 'Create!')
                : settings.t('다음', 'Next'),
          ),
        ),
      ),
    );
  }

  void _onNext(BuildContext context) {
    if (_step == 4 && !_nameEdited) {
      // Auto-fill suggested name when reaching name step
      _nameController.text = _getSuggestedName();
    }
    if (_step < _totalSteps - 1) {
      setState(() => _step++);
    } else {
      _createCharacter(context);
    }
  }

  void _createCharacter(BuildContext context) {
    final character = Character(
      id: _uuid.v4(),
      name: _nameController.text.trim(),
      relationshipId: _relationshipId!,
      personalityId: _personalityId!,
      speechStyleId: _speechStyleId!,
      interestIds: _interestIds.toList(),
      appearanceId: _appearanceId!,
      createdAt: DateTime.now(),
    );

    context.read<CharacterProvider>().addCharacter(character);
    Navigator.pop(context);

    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(
          context
              .read<SettingsProvider>()
              .t('${character.name} 친구가 생겼어요! 🎉',
                  '${character.name} is ready to chat! 🎉'),
        ),
        behavior: SnackBarBehavior.floating,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      ),
    );
  }
}

// ─── Category step widget ────────────────────────────────────────────────────
class _CategoryStep extends StatelessWidget {
  final String title;
  final String subtitle;
  final List<CategoryItem> items;
  final Set<String> selected;
  final String locale;
  final bool multiSelect;
  final void Function(String id) onToggle;

  const _CategoryStep({
    super.key,
    required this.title,
    required this.subtitle,
    required this.items,
    required this.selected,
    required this.locale,
    required this.multiSelect,
    required this.onToggle,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 24),
          child: Column(
            children: [
              Text(title,
                  style: Theme.of(context).textTheme.headlineMedium,
                  textAlign: TextAlign.center),
              const SizedBox(height: 6),
              Text(subtitle,
                  style: Theme.of(context).textTheme.bodyMedium,
                  textAlign: TextAlign.center),
            ],
          ),
        ).animate().fadeIn(duration: 300.ms),
        const SizedBox(height: 20),
        Expanded(
          child: GridView.builder(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
              crossAxisCount: 3,
              childAspectRatio: 1,
              crossAxisSpacing: 12,
              mainAxisSpacing: 12,
            ),
            itemCount: items.length,
            itemBuilder: (_, i) => CategoryOptionCard(
              item: items[i],
              selected: selected.contains(items[i].id),
              locale: locale,
              onTap: () => onToggle(items[i].id),
            ).animate(delay: (i * 50).ms).fadeIn().scale(begin: const Offset(0.9, 0.9)),
          ),
        ),
      ],
    );
  }
}

// ─── Name step widget ────────────────────────────────────────────────────────
class _NameStep extends StatefulWidget {
  final TextEditingController controller;
  final String suggestedName;
  final SettingsProvider settings;
  final VoidCallback onNameChanged;

  const _NameStep({
    super.key,
    required this.controller,
    required this.suggestedName,
    required this.settings,
    required this.onNameChanged,
  });

  @override
  State<_NameStep> createState() => _NameStepState();
}

class _NameStepState extends State<_NameStep> {
  @override
  void initState() {
    super.initState();
    if (widget.controller.text.isEmpty) {
      widget.controller.text = widget.suggestedName;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 24),
      child: Column(
        children: [
          Text(
            widget.settings.t('이름을 정해볼까요?', 'Name your friend!'),
            style: Theme.of(context).textTheme.headlineMedium,
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: 8),
          Text(
            widget.settings.t(
              '친구의 이름을 입력하거나 추천 이름을 사용하세요',
              'Enter a name or use the suggested one',
            ),
            style: Theme.of(context).textTheme.bodyMedium,
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: 32),
          TextField(
            controller: widget.controller,
            onChanged: (_) => widget.onNameChanged(),
            textAlign: TextAlign.center,
            style: const TextStyle(fontSize: 22, fontWeight: FontWeight.w700),
            decoration: InputDecoration(
              hintText: widget.settings.t('이름 입력...', 'Enter name...'),
            ),
          ),
          const SizedBox(height: 16),
          Text(
            widget.settings.t(
              '💡 추천: ${widget.suggestedName}',
              '💡 Suggested: ${widget.suggestedName}',
            ),
            style: const TextStyle(
              color: Color(0xFF7C5CBF),
              fontWeight: FontWeight.w500,
            ),
          ),
          TextButton(
            onPressed: () {
              widget.controller.text = widget.suggestedName;
              widget.onNameChanged();
            },
            child: Text(widget.settings.t('추천 이름 사용', 'Use suggested')),
          ),
        ],
      ),
    ).animate().fadeIn(duration: 300.ms);
  }
}
