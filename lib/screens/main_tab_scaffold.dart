import 'package:flutter/cupertino.dart';
import 'home_screen.dart';
import 'calendar_screen.dart';
import 'settings_screen.dart';

class MainTabScaffold extends StatelessWidget {
  const MainTabScaffold({super.key});

  @override
  Widget build(BuildContext context) {
    return CupertinoTabScaffold(
      tabBar: CupertinoTabBar(
        items: const [
          BottomNavigationBarItem(icon: Icon(CupertinoIcons.doc_text), label: 'Catatan'),
          BottomNavigationBarItem(icon: Icon(CupertinoIcons.calendar), label: 'Kalender'),
          BottomNavigationBarItem(icon: Icon(CupertinoIcons.settings), label: 'Pengaturan'),
        ],
      ),
      tabBuilder: (context, index) {
        switch (index) {
          case 0:
            return CupertinoTabView(builder: (_) => const HomeScreen());
          case 1:
            return CupertinoTabView(builder: (_) => const CalendarScreen());
          default:
            return CupertinoTabView(builder: (_) => const SettingsScreen());
        }
      },
    );
  }
}
