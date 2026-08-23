import 'package:flutter/cupertino.dart';
import 'package:provider/provider.dart';
import '../providers/settings_provider.dart';
import '../services/auth_service.dart';

enum LockMode { setup, verify }

class LockScreen extends StatefulWidget {
  final LockMode mode;
  const LockScreen({super.key, required this.mode});

  @override
  State<LockScreen> createState() => _LockScreenState();
}

class _LockScreenState extends State<LockScreen> {
  final _auth = AuthService();
  String _pin = '';
  String _confirmPin = '';
  bool _isConfirming = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    if (widget.mode == LockMode.verify) {
      WidgetsBinding.instance.addPostFrameCallback((_) => _tryBiometric());
    }
  }

  Future<void> _tryBiometric() async {
    final enabled = await _auth.isBiometricPreferenceEnabled();
    final available = await _auth.canUseBiometrics();
    if (enabled && available) {
      final ok = await _auth.authenticateBiometric();
      if (ok && mounted) Navigator.of(context).pop(true);
    }
  }

  void _onDigit(String digit) {
    setState(() {
      _error = null;
      if (widget.mode == LockMode.setup) {
        if (!_isConfirming) {
          if (_pin.length < 4) _pin += digit;
          if (_pin.length == 4) _isConfirming = true;
        } else {
          if (_confirmPin.length < 4) _confirmPin += digit;
          if (_confirmPin.length == 4) _finishSetup();
        }
      } else {
        if (_pin.length < 4) _pin += digit;
        if (_pin.length == 4) _verify();
      }
    });
  }

  Future<void> _finishSetup() async {
    if (_pin == _confirmPin) {
      await context.read<SettingsProvider>().enableLock(_pin);
      if (mounted) Navigator.of(context).pop(true);
    } else {
      setState(() {
        _error = 'PIN tidak cocok, coba lagi';
        _pin = '';
        _confirmPin = '';
        _isConfirming = false;
      });
    }
  }

  Future<void> _verify() async {
    final ok = await _auth.verifyPin(_pin);
    if (ok && mounted) {
      Navigator.of(context).pop(true);
    } else {
      setState(() {
        _error = 'PIN salah';
        _pin = '';
      });
    }
  }

  void _backspace() {
    setState(() {
      if (widget.mode == LockMode.setup && _isConfirming) {
        if (_confirmPin.isNotEmpty) _confirmPin = _confirmPin.substring(0, _confirmPin.length - 1);
      } else {
        if (_pin.isNotEmpty) _pin = _pin.substring(0, _pin.length - 1);
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    final activePin = (widget.mode == LockMode.setup && _isConfirming) ? _confirmPin : _pin;
    final title = widget.mode == LockMode.setup ? (_isConfirming ? 'Konfirmasi PIN' : 'Buat PIN Baru') : 'Masukkan PIN';

    return CupertinoPageScaffold(
      navigationBar: CupertinoNavigationBar(middle: Text(title), automaticallyImplyLeading: widget.mode == LockMode.setup),
      child: SafeArea(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: List.generate(4, (i) {
                final filled = i < activePin.length;
                return Container(
                  margin: const EdgeInsets.all(8),
                  width: 16,
                  height: 16,
                  decoration: BoxDecoration(
                      shape: BoxShape.circle, color: filled ? CupertinoColors.activeBlue : CupertinoColors.systemGrey4),
                );
              }),
            ),
            const SizedBox(height: 12),
            if (_error != null) Text(_error!, style: const TextStyle(color: CupertinoColors.systemRed)),
            const SizedBox(height: 24),
            _buildKeypad(),
          ],
        ),
      ),
    );
  }

  Widget _buildKeypad() {
    final keys = ['1', '2', '3', '4', '5', '6', '7', '8', '9', '', '0', '⌫'];
    return SizedBox(
      width: 260,
      child: GridView.count(
        crossAxisCount: 3,
        shrinkWrap: true,
        physics: const NeverScrollableScrollPhysics(),
        children: keys.map((k) {
          if (k.isEmpty) return const SizedBox();
          return CupertinoButton(
            onPressed: () => k == '⌫' ? _backspace() : _onDigit(k),
            child: Text(k, style: const TextStyle(fontSize: 24)),
          );
        }).toList(),
      ),
    );
  }
}
