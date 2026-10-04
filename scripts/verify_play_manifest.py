#!/usr/bin/env python3
"""Verify bundletool's decoded release manifest before publishing an artifact."""
import sys
import xml.etree.ElementTree as ET


def verify(root, expected_version):
    android = '{http://schemas.android.com/apk/res/android}'
    if root.get('package') != 'com.iumrah.app':
        raise ValueError('Release package must be com.iumrah.app')
    if not 14 < expected_version <= 2100000000:
        raise ValueError('Invalid release version code')
    if root.get(android + 'versionCode') != str(expected_version):
        raise ValueError('Bundle version code differs from selected version')
    application = root.find('application')
    if application is None or application.get(android + 'debuggable', 'false') != 'false':
        raise ValueError('Bundle must be non-debuggable')
    if application.get(android + 'testOnly', 'false') != 'false':
        raise ValueError('Bundle must not be test-only')
    for node in root.iter():
        name = node.get(android + 'name', '')
        if name == 'com.android.vending.BILLING' or name.startswith('com.android.billingclient.'):
            raise ValueError('Google Play Billing must not be present')


if __name__ == '__main__':
    try:
        verify(ET.parse(sys.argv[1]).getroot(), int(sys.argv[2]))
    except (ValueError, ET.ParseError, OSError, IndexError) as error:
        raise SystemExit(str(error))
    print('Verified: com.iumrah.app, selected version, release flags, no Billing permission/metadata.')
