#!/usr/bin/env python3
"""Verify bundletool's decoded release manifest before publishing an artifact."""
import sys
import xml.etree.ElementTree as ET

ANDROID = '{http://schemas.android.com/apk/res/android}'


def verify(root, expected_version):
    if root.get('package') != 'com.iumrah.app':
        raise ValueError('Release package must be com.iumrah.app')
    if not 14 < expected_version <= 2100000000:
        raise ValueError('Invalid release version code')
    if root.get(ANDROID + 'versionCode') != str(expected_version):
        raise ValueError('Bundle version code differs from selected version')

    uses_sdk = root.find('uses-sdk')
    if uses_sdk is None:
        raise ValueError('uses-sdk missing from decoded release manifest')
    if uses_sdk.get(ANDROID + 'targetSdkVersion') != '36':
        raise ValueError('Release targetSdkVersion must be 36')
    min_sdk = uses_sdk.get(ANDROID + 'minSdkVersion')
    if min_sdk not in {'26', '27', '28', '29', '30', '31', '32', '33', '34', '35', '36'}:
        raise ValueError('Release minSdkVersion must be at least 26')

    application = root.find('application')
    if application is None:
        raise ValueError('application node missing')
    if application.get(ANDROID + 'debuggable', 'false') != 'false':
        raise ValueError('Bundle must be non-debuggable')
    if application.get(ANDROID + 'testOnly', 'false') != 'false':
        raise ValueError('Bundle must not be test-only')
    if application.get(ANDROID + 'allowBackup', 'false') != 'false':
        raise ValueError('Production backup must be disabled')
    if application.get(ANDROID + 'usesCleartextTraffic', 'false') != 'false':
        raise ValueError('Production cleartext traffic must be disabled')

    for node in root.iter():
        name = node.get(ANDROID + 'name', '')
        if name == 'com.android.vending.BILLING' or name.startswith('com.android.billingclient.'):
            raise ValueError('Google Play Billing must not be present')


if __name__ == '__main__':
    try:
        verify(ET.parse(sys.argv[1]).getroot(), int(sys.argv[2]))
    except (ValueError, ET.ParseError, OSError, IndexError) as error:
        raise SystemExit(str(error))
    print('Verified: com.iumrah.app, SDK 26+/36, selected version, release flags, no Billing.')
