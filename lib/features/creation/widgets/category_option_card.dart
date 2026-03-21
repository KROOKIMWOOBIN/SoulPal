import 'package:flutter/material.dart';
import '../../../core/constants/categories.dart';

class CategoryOptionCard extends StatelessWidget {
  final CategoryItem item;
  final bool selected;
  final String locale;
  final VoidCallback onTap;

  const CategoryOptionCard({
    super.key,
    required this.item,
    required this.selected,
    required this.locale,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 200),
        decoration: BoxDecoration(
          color: selected
              ? item.color.withOpacity(0.2)
              : Colors.white,
          borderRadius: BorderRadius.circular(18),
          border: Border.all(
            color: selected ? item.color : const Color(0xFFE8E0F0),
            width: selected ? 2.5 : 1.5,
          ),
          boxShadow: selected
              ? [
                  BoxShadow(
                    color: item.color.withOpacity(0.25),
                    blurRadius: 12,
                    offset: const Offset(0, 4),
                  )
                ]
              : [],
        ),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text(item.emoji, style: const TextStyle(fontSize: 36)),
            const SizedBox(height: 6),
            Text(
              item.label(locale),
              textAlign: TextAlign.center,
              style: TextStyle(
                fontSize: 13,
                fontWeight: selected ? FontWeight.w700 : FontWeight.w500,
                color: selected ? item.color : const Color(0xFF2D2040),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
