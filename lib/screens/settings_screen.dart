import 'package:flutter/cupertino.dart';
import 'package:provider/provider.dart';
import '../providers/settings_provider.dart';
import '../services/auth_service.dart';
import 'lock_screen.dart';
import 'filtered_notes_screen.dart';

class SettingsScreen extends StatefulWidget {
  const SettingsScreen({super.key});

  @override
  State<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends State<SettingsScreen> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      context.read<SettingsProvider>().load();
    });
  }

  @override
  Widget build(BuildContext context) {
    final settings = context.watch<SettingsProvider>();
    return CupertinoPageScaffold(
      navigationBar: const CupertinoNavigationBar(middle: Text('Pengaturan')),
      child: SafeArea(
        child: ListView(
          children: [
            CupertinoListSection.insetGrouped(
              header: const Text('KEAMANAN'),
              children: [
                CupertinoListTile(
                  title: const Text('Kunci Aplikasi (PIN)'),
                  trailing: CupertinoSwitch(
                    value: settings.isLockEnabled,
                    onChanged: (v) async {
                      if (v) {
                        final ok = await Navigator.of(context).push<bool>(
                          CupertinoPageRoute(builder: (_) => const LockScreen(mode: LockMode.setup)),
                        );
                        if (ok == true) settings.load();
                      } else {
                        await settings.disableLock();
                      }
                    },
                  ),
                ),
                if (settings.isLockEnabled)
                  CupertinoListTile(
                    title: const Text('Gunakan Biometrik'),
                    trailing: CupertinoSwitch(
                      value: settings.isBiometricEnabled,
                      onChanged: (v) async {
                        if (v) {
                          final available = await AuthService().canUseBiometrics();
                          if (!available) {
                            if (mounted) {
                              showCupertinoDialog(
                                context: context,
                                builder: (_) => CupertinoAlertDialog(
                                  title: const Text('Biometrik Tidak Tersedia'),
                                  content: const Text(
                                      'Pastikan sidik jari atau Face Unlock sudah didaftarkan di pengaturan HP Anda, lalu coba lagi.'),
                                  actions: [
                                    CupertinoDialogAction(
                                      child: const Text('OK'),
                                      onPressed: () => Navigator.pop(context),
                                    ),
                                  ],
                                ),
                              );
                            }
                            return;
                          }
                        }
                        settings.setBiometricEnabled(v);
                      },
                    ),
                  ),
              ],
            ),
            CupertinoListSection.insetGrouped(
              header: const Text('CATATAN'),
              children: [
                CupertinoListTile(
                  title: const Text('Arsip'),
                  leading: const Icon(CupertinoIcons.archivebox),
                  trailing: const Icon(CupertinoIcons.chevron_right, size: 18),
                  onTap: () => Navigator.of(context)
                      .push(CupertinoPageRoute(builder: (_) => const FilteredNotesScreen(mode: FilteredMode.archive))),
                ),
                CupertinoListTile(
                  title: const Text('Sampah'),
                  leading: const Icon(CupertinoIcons.trash),
                  trailing: const Icon(CupertinoIcons.chevron_right, size: 18),
                  onTap: () => Navigator.of(context)
                      .push(CupertinoPageRoute(builder: (_) => const FilteredNotesScreen(mode: FilteredMode.trash))),
                ),
              ],
            ),
            const Padding(
              padding: EdgeInsets.all(16),
              child: Text('Jotter v1.0 · 100% offline · Log crash tersimpan di Documents/Jotter/logs',
                  style: TextStyle(color: CupertinoColors.systemGrey, fontSize: 12), textAlign: TextAlign.center),
            ),
          ],
        ),
      ),
    );
  }
}
