import fs from 'fs';
import path from 'path';
import { execSync } from 'child_process';
import { memoryService } from './MemoryService';

export interface AndroidRunPlan {
  command: string;
  cwd: string;
  applicationId?: string;
  launchEnabled: boolean;
  runner: string;
  requiresPhysicalDevice: boolean;
  deviceId?: string;
}

export interface AndroidSyncPlan {
  command: string;
  cwd: string;
  runner: string;
}

export interface AndroidDeviceInfo {
  id: string;
  status: string;
}

export class AndroidService {
  private sanitizeDeviceId(deviceId?: string): string | undefined {
    if (!deviceId) return undefined;
    return /^[A-Za-z0-9._:-]+$/.test(deviceId) ? deviceId : undefined;
  }

  private resolvePreferredDeviceId(deviceId?: string): string | undefined {
    const sanitizedDeviceId = this.sanitizeDeviceId(deviceId);
    if (sanitizedDeviceId) return sanitizedDeviceId;

    return this.listConnectedDevices().find((device) => (
      device.status === 'device'
      && !device.id.startsWith('emulator-')
      && !device.id.startsWith('localhost:')
    ))?.id;
  }

  private buildGradleUserHome(cwd: string): string {
    return path.join(cwd, '.gradle-user-home');
  }

  private wrapGradleCommand(baseCommand: string, cwd: string): string {
    const gradleUserHome = this.buildGradleUserHome(cwd);
    if (process.platform === 'win32') {
      return `set "GRADLE_USER_HOME=${gradleUserHome}" && ${baseCommand}`;
    }
    return `GRADLE_USER_HOME="${gradleUserHome}" ${baseCommand}`;
  }

  private buildDeviceAwareGradleCommand(baseCommand: string, cwd: string, deviceId?: string): string {
    const gradleUserHome = this.buildGradleUserHome(cwd);
    const sanitizedDeviceId = this.sanitizeDeviceId(deviceId);

    if (process.platform === 'win32') {
      if (sanitizedDeviceId) {
        return [
          `set "ANDROID_SERIAL=${sanitizedDeviceId}"`,
          `set "GRADLE_USER_HOME=${gradleUserHome}"`,
          `echo Using Android device ${sanitizedDeviceId}`,
          baseCommand,
        ].join(' && ');
      }

      return [
        'setlocal EnableDelayedExpansion',
        `for /f "tokens=1" %i in ('adb devices ^| findstr /R /C:".*device$"') do if not defined ANDROID_SERIAL set "ANDROID_SERIAL=%i"`,
        'if not defined ANDROID_SERIAL (echo No eligible Android device detected. Current adb devices output: & adb devices & exit /b 1)',
        `set "GRADLE_USER_HOME=${gradleUserHome}"`,
        'echo Using Android device !ANDROID_SERIAL!',
        baseCommand,
      ].join(' && ');
    }

    if (sanitizedDeviceId) {
      return [
        `ANDROID_SERIAL="${sanitizedDeviceId}"`,
        `export GRADLE_USER_HOME="${gradleUserHome}"`,
        'export ANDROID_SERIAL',
        'echo "Using Android device $ANDROID_SERIAL"',
        baseCommand,
      ].join(' && ');
    }

    return [
      'ANDROID_SERIAL="$(adb devices | awk \'NR > 1 && $2 == "device" && $1 !~ /^emulator-/ && $1 !~ /^localhost:/ { print $1; exit }\')"',
      'if [ -z "$ANDROID_SERIAL" ]; then echo "No eligible Android device detected. Current adb devices output:"; adb devices; exit 1; fi',
      `export GRADLE_USER_HOME="${gradleUserHome}"`,
      'export ANDROID_SERIAL',
      'echo "Using Android device $ANDROID_SERIAL"',
      baseCommand,
    ].join(' && ');
  }

  private buildLaunchVerificationCommand(applicationId: string, deviceId?: string): string {
    const sanitizedDeviceId = this.sanitizeDeviceId(deviceId);

    if (process.platform === 'win32') {
      const deviceRef = sanitizedDeviceId ? sanitizedDeviceId : '!ANDROID_SERIAL!';
      return [
        sanitizedDeviceId ? '' : 'if not defined ANDROID_SERIAL (echo Device serial was not resolved. & exit /b 1)',
        `adb -s ${deviceRef} shell monkey -p ${applicationId} -c android.intent.category.LAUNCHER 1`,
        'timeout /t 2 /nobreak >nul',
        `adb -s ${deviceRef} shell pidof ${applicationId} >nul 2>&1 && (echo App launch verified on device ${deviceRef} via pidof. & exit /b 0)`,
        'timeout /t 2 /nobreak >nul',
        `adb -s ${deviceRef} shell pidof ${applicationId} >nul 2>&1 && (echo App launch verified on device ${deviceRef} via pidof retry 1. & exit /b 0)`,
        'timeout /t 2 /nobreak >nul',
        `adb -s ${deviceRef} shell pidof ${applicationId} >nul 2>&1 && (echo App launch verified on device ${deviceRef} via pidof retry 2. & exit /b 0)`,
        `adb -s ${deviceRef} shell dumpsys activity activities | findstr /i "${applicationId}" >nul 2>&1 && (echo App launch verified on device ${deviceRef} via dumpsys. & exit /b 0)`,
        `echo App launch could not be verified on device ${deviceRef}. The device is connected, but the app process was not detected after launch.`,
        'exit /b 1',
      ].filter(Boolean).join(' & ');
    }

    const deviceRef = sanitizedDeviceId ? sanitizedDeviceId : '$ANDROID_SERIAL';
    return [
      sanitizedDeviceId ? '' : 'if [ -z "$ANDROID_SERIAL" ]; then echo "Device serial was not resolved."; exit 1; fi',
      `adb -s "${deviceRef}" shell monkey -p ${applicationId} -c android.intent.category.LAUNCHER 1`,
      'sleep 2',
      `adb -s "${deviceRef}" shell pidof ${applicationId} >/dev/null 2>&1 && echo "App launch verified on device ${deviceRef} via pidof." && exit 0`,
      'sleep 2',
      `adb -s "${deviceRef}" shell pidof ${applicationId} >/dev/null 2>&1 && echo "App launch verified on device ${deviceRef} via pidof retry 1." && exit 0`,
      'sleep 2',
      `adb -s "${deviceRef}" shell pidof ${applicationId} >/dev/null 2>&1 && echo "App launch verified on device ${deviceRef} via pidof retry 2." && exit 0`,
      `adb -s "${deviceRef}" shell dumpsys activity activities | grep -i "${applicationId}" >/dev/null 2>&1 && echo "App launch verified on device ${deviceRef} via dumpsys." && exit 0`,
      `echo "App launch could not be verified on device ${deviceRef}. The device is connected, but the app process was not detected after launch."`,
      'exit 1',
    ].filter(Boolean).join(' ; ');
  }

  private sanitizeApplicationId(applicationId?: string): string | undefined {
    if (!applicationId) return undefined;
    return /^[A-Za-z0-9_.]+$/.test(applicationId) ? applicationId : undefined;
  }

  isAndroidProject(cwd: string): boolean {
    return memoryService.detectProjectType(cwd) === 'android';
  }

  listConnectedDevices(): AndroidDeviceInfo[] {
    try {
      const output = execSync('adb devices', { encoding: 'utf8', windowsHide: true });
      return output
        .split(/\r?\n/)
        .slice(1)
        .map((line) => line.trim())
        .filter(Boolean)
        .map((line) => {
          const parts = line.split(/\s+/);
          return {
            id: parts[0],
            status: parts[1] || 'unknown',
          };
        })
        .filter((device) => device.id && device.id !== 'List');
    } catch {
      return [];
    }
  }

  private resolveGradleCommand(cwd: string, task: string): { runner: string; command: string } {
    if (!this.isAndroidProject(cwd)) {
      throw new Error(`Current directory is not an Android project: ${cwd}`);
    }

    const isWindows = process.platform === 'win32';
    const gradleWrapperWindows = path.join(cwd, 'gradlew.bat');
    const gradleWrapperUnix = path.join(cwd, 'gradlew');

    let runner = 'gradle';
    let installCommand = 'gradle installDebug';

    if (isWindows && fs.existsSync(gradleWrapperWindows)) {
      runner = 'gradlew.bat';
      installCommand = `gradlew.bat ${task}`;
    } else if (!isWindows && fs.existsSync(gradleWrapperUnix)) {
      runner = './gradlew';
      installCommand = `./gradlew ${task}`;
    } else if (fs.existsSync(gradleWrapperWindows)) {
      runner = 'gradlew.bat';
      installCommand = `gradlew.bat ${task}`;
    } else if (fs.existsSync(gradleWrapperUnix)) {
      runner = './gradlew';
      installCommand = `./gradlew ${task}`;
    }

    return {
      runner,
      command: this.wrapGradleCommand(installCommand, cwd),
    };
  }

  buildRunAppCommand(cwd: string, deviceId?: string): AndroidRunPlan {
    const gradle = this.resolveGradleCommand(cwd, 'installDebug');
    const resolvedDeviceId = this.resolvePreferredDeviceId(deviceId);
    const installCommand = this.buildDeviceAwareGradleCommand(gradle.command.replace(/^set "GRADLE_USER_HOME=.*?" && /, '').replace(/^GRADLE_USER_HOME=".*?" /, ''), cwd, resolvedDeviceId);

    const applicationId = this.sanitizeApplicationId(memoryService.extractAndroidAppId(cwd));
    if (!applicationId) {
      return {
        command: `${installCommand} && echo Installed debug build, but launch was skipped because applicationId was not detected.`,
          cwd,
          launchEnabled: false,
          runner: gradle.runner,
          requiresPhysicalDevice: true,
          deviceId: resolvedDeviceId,
      };
    }

    return {
      command: `${installCommand} && ${this.buildLaunchVerificationCommand(applicationId, resolvedDeviceId)}`,
      cwd,
      applicationId,
      launchEnabled: true,
      runner: gradle.runner,
      requiresPhysicalDevice: true,
      deviceId: resolvedDeviceId,
    };
  }

  buildSyncProjectCommand(cwd: string): AndroidSyncPlan {
    const gradle = this.resolveGradleCommand(cwd, 'help');
    return {
      command: gradle.command,
      cwd,
      runner: gradle.runner,
    };
  }
}

export const androidService = new AndroidService();
