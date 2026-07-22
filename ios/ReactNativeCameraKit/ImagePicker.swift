import PhotosUI
import UIKit

@objc class ImagePicker: NSObject {
    @objc static func pickImage(
        from presentingViewController: UIViewController,
        completion: @escaping (URL?) -> Void
    ) {
        if #available(iOS 14, *) {
            var configuration = PHPickerConfiguration()
            configuration.filter = .images
            configuration.selectionLimit = 1

            let picker = PHPickerViewController(configuration: configuration)
            picker.delegate = ImagePickerDelegate.shared
            ImagePickerDelegate.shared.completion = { result in
                DispatchQueue.main.async {
                    picker.dismiss(animated: true) {
                        completion(result)
                    }
                }
            }
            presentingViewController.present(picker, animated: true)
        } else {
            let picker = UIImagePickerController()
            picker.sourceType = .photoLibrary
            picker.delegate = ImagePickerDelegate.shared
            ImagePickerDelegate.shared.completion = { result in
                DispatchQueue.main.async {
                    picker.dismiss(animated: true) {
                        completion(result)
                    }
                }
            }
            presentingViewController.present(picker, animated: true)
        }
    }
}

@objc class ImagePickerDelegate: NSObject, PHPickerViewControllerDelegate, UIImagePickerControllerDelegate, UINavigationControllerDelegate {
    static let shared = ImagePickerDelegate()
    var completion: ((URL?) -> Void)?

    func picker(_ picker: PHPickerViewController, didFinishPicking results: [PHPickerResult]) {
        guard let result = results.first else {
            completion?(nil)
            return
        }

        let itemProvider = result.itemProvider
        guard itemProvider.canLoadObject(ofClass: UIImage.self) else {
            completion?(nil)
            return
        }

        itemProvider.loadObject(ofClass: UIImage.self) { [weak self] object, error in
            guard let self, let image = object as? UIImage else {
                self?.completion?(nil)
                return
            }

            let tempDir = FileManager.default.temporaryDirectory
            let fileName = UUID().uuidString + ".jpg"
            let fileURL = tempDir.appendingPathComponent(fileName)

            guard let data = image.jpegData(compressionQuality: 0.9) else {
                self.completion?(nil)
                return
            }

            do {
                try data.write(to: fileURL)
                self.completion?(fileURL)
            } catch {
                self.completion?(nil)
            }
        }
    }

    func imagePickerController(
        _ picker: UIImagePickerController,
        didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey: Any]
    ) {
        guard let image = info[.originalImage] as? UIImage else {
            completion?(nil)
            return
        }

        let tempDir = FileManager.default.temporaryDirectory
        let fileName = UUID().uuidString + ".jpg"
        let fileURL = tempDir.appendingPathComponent(fileName)

        guard let data = image.jpegData(compressionQuality: 0.9) else {
            completion?(nil)
            return
        }

        do {
            try data.write(to: fileURL)
            completion?(fileURL)
        } catch {
            completion?(nil)
        }
    }

    func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
        completion?(nil)
    }
}
