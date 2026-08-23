import 'package:flutter/foundation.dart';
import '../services/auth_service.dart';

class SettingsProvider extends ChangeNotifier {
  final AuthService _auth = AuthService();
  bool isLockEnabled = false;
  bool isBiometricEnabled = false;

  Future<void> load() async {
    isLockEnabled = await _auth.hasPinSet();
    isBiometricEnabled = await _auth.isBiometricPreferenceEnabled();
    notifyListeners();
  }

  Future<void> enableLock(String pin) async {
    await _auth.setPin(pin);
    isLockEnabled = true;
    notifyListeners();
  }

  Future<void> disableLock() async {
    await _auth.clearPin();
    isLockEnabled = false;
    isBiometricEnabled = false;
    notifyListeners();
  }

  Future<void> setBiometricEnabled(bool value) async {
    await _auth.setBiometricPreference(value);
    isBiometricEnabled = value;
    notifyListeners();
  }
}
