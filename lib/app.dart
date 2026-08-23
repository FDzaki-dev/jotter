import 'package:flutter/cupertino.dart';
import 'package:provider/provider.dart';
import 'providers/notes_provider.dart';
import 'providers/settings_provider.dart';
import 'services/auth_service.dart';
import 'services/notification_service.dart';
import 'screens/main_tab_scaffold.dart';
import 'screens/lock_screen.dart';

class JotterApp extends StatelessWidget {
  const JotterApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MultiProvider(
      providers: [
        ChangeNotifierProvider(create: (_) => NotesProvider()),
        ChangeNotifierProvider(create: (_) => SettingsProvider()),
      ],
      child: CupertinoApp(
        title: 'Jotter',
        debugShowCheckedModeBanner: false,
        theme: const CupertinoThemeData(primaryColor: CupertinoColors.activeBlue),
        builder: (context, child) => Material(type: MaterialType.transparency, child: child),
        home: const _AppEntry(),
      ),
    );
  }
}

class _AppEntry extends StatefulWidget {
  const _AppEntry();

  @override
  State<_AppEntry> createState() => _AppEntryState();
}

class _AppEntryState extends State<_AppEntry> {
  bool _loading = true;
  bool _hasPin = false;
  bool _unlocked = false;

  @override
  void initState() {
    super.initState();
    _init();
  }

  Future<void> _init() async {
    await NotificationService().init();
    _hasPin = await AuthService().hasPinSet();
    if (mounted) setState(() => _loading = false);
    if (_hasPin) {
      WidgetsBinding.instance.addPostFrameCallback((_) => _requestUnlock());
    }
  }

  Future<void> _requestUnlock() async {
    final result = await Navigator.of(context).push<bool>(
      CupertinoPageRoute(builder: (_) => const LockScreen(mode: LockMode.verify)),
    );
    if (mounted) setState(() => _unlocked = result == true);
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) {
      return const CupertinoPageScaffold(child: Center(child: CupertinoActivityIndicator()));
    }
    if (_hasPin && !_unlocked) {
      return CupertinoPageScaffold(
        child: Center(child: CupertinoButton(onPressed: _requestUnlock, child: const Text('Buka Kunci'))),
      );
    }
    return const MainTabScaffold();
  }
}
