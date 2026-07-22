import Foundation
import Vision
import CoreImage

@objc class ImageScanner: NSObject {
    @objc static func scanBarcodes(
        from imageURL: URL,
        allowedBarcodeTypes: [String]?,
        completion: @escaping ([[String: Any]]) -> Void
    ) {
        guard let ciImage = CIImage(contentsOf: imageURL) else {
            completion([])
            return
        }

        let request = VNDetectBarcodesRequest { request, error in
            guard error == nil else {
                completion([])
                return
            }

            let barcodes = (request.results as? [VNBarcodeObservation]) ?? []
            let filtered = barcodes.filter { observation in
                if let allowedTypes = allowedBarcodeTypes, !allowedTypes.isEmpty {
                    let format = CodeFormat.fromVNBarcodeSymbology(observation.symbology)
                    return allowedTypes.contains(format.rawValue)
                }
                return true
            }

            let results: [[String: Any]] = filtered.compactMap { observation in
                guard let value = observation.payloadStringValue else { return nil }

                let format = CodeFormat.fromVNBarcodeSymbology(observation.symbology)
                let boundingBox = observation.boundingBox

                return [
                    "codeStringValue": value,
                    "codeFormat": format.rawValue,
                    "displayValue": value,
                    "boundingBox": [
                        "x": boundingBox.origin.x,
                        "y": boundingBox.origin.y,
                        "width": boundingBox.size.width,
                        "height": boundingBox.size.height,
                    ] as [String: Double],
                ]
            }

            completion(results)
        }

        let handler = VNImageRequestHandler(ciImage: ciImage, options: [:])
        do {
            try handler.perform([request])
        } catch {
            completion([])
        }
    }

    @objc static func scanBarcodes(
        from imageData: Data,
        allowedBarcodeTypes: [String]?,
        completion: @escaping ([[String: Any]]) -> Void
    ) {
        guard let ciImage = CIImage(data: imageData) else {
            completion([])
            return
        }

        let request = VNDetectBarcodesRequest { request, error in
            guard error == nil else {
                completion([])
                return
            }

            let barcodes = (request.results as? [VNBarcodeObservation]) ?? []
            let filtered = barcodes.filter { observation in
                if let allowedTypes = allowedBarcodeTypes, !allowedTypes.isEmpty {
                    let format = CodeFormat.fromVNBarcodeSymbology(observation.symbology)
                    return allowedTypes.contains(format.rawValue)
                }
                return true
            }

            let results: [[String: Any]] = filtered.compactMap { observation in
                guard let value = observation.payloadStringValue else { return nil }

                let format = CodeFormat.fromVNBarcodeSymbology(observation.symbology)
                let boundingBox = observation.boundingBox

                return [
                    "codeStringValue": value,
                    "codeFormat": format.rawValue,
                    "displayValue": value,
                    "boundingBox": [
                        "x": boundingBox.origin.x,
                        "y": boundingBox.origin.y,
                        "width": boundingBox.size.width,
                        "height": boundingBox.size.height,
                    ] as [String: Double],
                ]
            }

            completion(results)
        }

        let handler = VNImageRequestHandler(ciImage: ciImage, options: [:])
        do {
            try handler.perform([request])
        } catch {
            completion([])
        }
    }
}
