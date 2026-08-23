import 'dart:convert';
import 'dart:math';
import 'package:crypto/crypto.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:local_auth/local_auth.dart';

class AuthService {
  static const _storage = FlutterSecureStorage();
  static const _kPinHashKey = 'pin_hash';
  static const _kPinSaltKey = 'pin_salt';
  static const _kBiometricKey = 'biometric_enabled';
  final LocalAuthentication _localAuth = LocalAuthentication();

  Future<bool> hasPinSet() async {
    final hash = await _storage.read(key: _kPinHashKey);
    return hash != null && hash.isNotEmpty;
  }

  Future<void> setPin(String pin) async {
    final salt = _generateSalt();
    final hash = _hashPin(pin, salt);
    await _storage.write(key: _kPinSaltKey, value: salt);
    await _storage.write(key: _kPinHashKey, value: hash);
  }

  Future<bool> verifyPin(String pin) async {
    final salt = await _storage.read(key: _kPinSaltKey);
    final storedHash = await _storage.read(key: _kPinHashKey);
    if (salt == null || storedHash == null) return false;
    return _hashPin(pin, salt) == storedHash;
  }

  Future<void> clearPin() async {
    await _storage.delete(key: _kPinHashKey);
    await _storage.delete(key: _kPinSaltKey);
    await _storage.delete(key: _kBiometricKey);
  }

  Future<bool> isBiometricPreferenceEnabled() async {
    final v = await _storage.read(key: _kBiometricKey);
    return v == 'true';
  }

  Future<void> setBiometricPreference(bool value) async {
    await _storage.write(key: _kBiometricKey, value: value.toString());
  }

  Future<bool> canUseBiometrics() async {
    try {
      final supported = await _localAuth.isDeviceSupported();
      final canCheck = await _localAuth.canCheckBiometrics;
      return supported && canCheck;
    } catch (_) {
      return false;
    }
  }

  Future<bool> authenticateBiometric() async {
    try {
      return await _localAuth.authenticate(
        localizedReason: 'Verifikasi untuk membuka catatan terkunci',
        options: const AuthenticationOptions(biometricOnly: true, stickyAuth: true),
      );
    } catch (_) {
      return false;
    }
  }

  String _generateSalt() {
    final rnd = Random.secure();
    final bytes = List<int>.generate(16, (_) => rnd.nextInt(256));
    return base64UrlEncode(bytes);
  }

  String _hashPin(String pin, String salt) {
    final bytes = utf8.encode('$salt:$pin');
    return sha256.convert(bytes).toString();
  }
}
