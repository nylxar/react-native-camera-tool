import React, { useRef, useState } from 'react';
import { StyleSheet, Text, View, TouchableOpacity, StatusBar } from 'react-native';
import Camera from '../../src/Camera';
import { type CameraApi, type ScannedBarcode } from '../../src/types';
import SafeAreaView from './SafeAreaView';

const GalleryScanExample = ({ onBack }: { onBack: () => void }) => {
  const cameraRef = useRef<CameraApi>(null);
  const [result, setResult] = useState<ScannedBarcode[] | null>(null);
  const [scanning, setScanning] = useState(false);

  const handlePickAndScan = async () => {
    try {
      setScanning(true);
      setResult(null);
      const results = await cameraRef.current?.pickAndScan({
        allowedBarcodeTypes: ['qr', 'ean-13', 'ean-8', 'code-128'],
      });
      setResult(results ?? []);
    } catch (error: any) {
      if (error.code !== 'E_PICKER_CANCELLED') {
        console.error('Scan error:', error);
      }
    } finally {
      setScanning(false);
    }
  };

  return (
    <View style={styles.screen}>
      <StatusBar hidden />
      <SafeAreaView style={styles.topButtons}>
        <TouchableOpacity style={styles.topButton} onPress={onBack}>
          <Text style={styles.buttonText}>Back</Text>
        </TouchableOpacity>
      </SafeAreaView>

      <View style={styles.content}>
        <Camera
          ref={cameraRef}
          style={styles.camera}
          cameraType={'back' as any}
        />

        <View style={styles.resultContainer}>
          {scanning ? (
            <Text style={styles.statusText}>Scanning...</Text>
          ) : result ? (
            result.length > 0 ? (
              result.map((barcode, index) => (
                <View key={index} style={styles.resultItem}>
                  <Text style={styles.resultFormat}>{barcode.codeFormat}</Text>
                  <Text style={styles.resultValue}>{barcode.codeStringValue}</Text>
                  <Text style={styles.resultDisplay}>{barcode.displayValue}</Text>
                </View>
              ))
            ) : (
              <Text style={styles.statusText}>No barcodes found</Text>
            )
          ) : (
            <Text style={styles.statusText}>Tap the button below to scan from gallery</Text>
          )}
        </View>

        <TouchableOpacity
          style={[styles.scanButton, scanning && styles.scanButtonDisabled]}
          onPress={handlePickAndScan}
          disabled={scanning}
        >
          <Text style={styles.scanButtonText}>
            {scanning ? 'Scanning...' : 'Scan from Gallery'}
          </Text>
        </TouchableOpacity>
      </View>
    </View>
  );
};

export default GalleryScanExample;

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: 'black',
  },
  topButtons: {
    margin: 10,
    zIndex: 10,
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  topButton: {
    backgroundColor: '#222',
    paddingHorizontal: 16,
    paddingVertical: 10,
    borderRadius: 20,
  },
  buttonText: {
    color: 'white',
    fontSize: 16,
  },
  content: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 20,
  },
  camera: {
    width: 200,
    height: 200,
    borderRadius: 12,
    overflow: 'hidden',
  },
  resultContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    maxHeight: 200,
  },
  resultItem: {
    backgroundColor: '#222',
    padding: 16,
    borderRadius: 12,
    marginVertical: 8,
    width: '100%',
    alignItems: 'center',
  },
  resultFormat: {
    color: '#007AFF',
    fontSize: 14,
    fontWeight: '600',
    textTransform: 'uppercase',
  },
  resultValue: {
    color: 'white',
    fontSize: 18,
    fontWeight: 'bold',
    marginTop: 4,
  },
  resultDisplay: {
    color: '#999',
    fontSize: 14,
    marginTop: 4,
  },
  statusText: {
    color: '#999',
    fontSize: 16,
  },
  scanButton: {
    backgroundColor: '#007AFF',
    paddingHorizontal: 32,
    paddingVertical: 16,
    borderRadius: 12,
    marginVertical: 20,
  },
  scanButtonDisabled: {
    backgroundColor: '#555',
  },
  scanButtonText: {
    color: 'white',
    fontSize: 18,
    fontWeight: '600',
  },
});
