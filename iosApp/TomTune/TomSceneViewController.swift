import UIKit
import RealityKit
import simd

/// Non-AR, fixed-camera scene. World x/z matches the shared screen x/y lug convention.
final class TomSceneViewController: UIViewController {
    var onSelect: ((Int) -> Void)?
    private var arView: ARView!
    private let anchor = AnchorEntity(world: .zero)
    private let drum = Entity()
    private var lugCount = 8
    private var selectedLug = 0
    private var caps: [ModelEntity] = []
    private var halos: [ModelEntity] = []
    private var builtCount = 0
    private let metal = SimpleMaterial(color: UIColor(white: 0.72, alpha: 1), roughness: 0.23, isMetallic: true)
    private let selected = SimpleMaterial(color: UIColor(red: 0.02, green: 0.30, blue: 1, alpha: 1), roughness: 0.25, isMetallic: true)

    override func loadView() {
        arView = ARView(frame: .zero, cameraMode: .nonAR, automaticallyConfigureSession: false)
        arView.environment.background = .color(UIColor(red: 0.86, green: 0.84, blue: 0.80, alpha: 1))
        view = arView
        arView.scene.addAnchor(anchor)
        anchor.addChild(drum)
        let camera = PerspectiveCamera()
        camera.camera.fieldOfViewInDegrees = 22.3
        camera.look(at: [0, -0.18, 0], from: [0, 5.6, 3.2], relativeTo: nil)
        anchor.addChild(camera)
        let light = DirectionalLight()
        light.light.intensity = 7000
        light.light.color = .white
        light.shadow = DirectionalLightComponent.Shadow(maximumDistance: 8, depthBias: 1)
        light.look(at: .zero, from: [-2, 4, -2], relativeTo: nil)
        anchor.addChild(light)
        let fill = PointLight()
        fill.light.intensity = 1100
        fill.light.attenuationRadius = 8
        fill.position = [2, 3, 2]
        anchor.addChild(fill)
        arView.addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(tapped(_:))))
        rebuild()
    }

    func setState(lugCount: Int, selectedLug: Int) {
        precondition(lugCount >= 3 && selectedLug >= 0 && selectedLug < lugCount)
        self.lugCount = lugCount
        self.selectedLug = selectedLug
        guard isViewLoaded else { return }
        if builtCount != lugCount { rebuild() }
        applySelection()
    }

    private func rebuild() {
        drum.children.forEach { $0.removeFromParent() }
        caps = []
        halos = []
        builtCount = lugCount
        let wood = SimpleMaterial(color: UIColor(red: 0.40, green: 0.23, blue: 0.12, alpha: 1), roughness: 0.42, isMetallic: false)
        let ivory = SimpleMaterial(color: UIColor(red: 0.89, green: 0.87, blue: 0.79, alpha: 1), roughness: 0.87, isMetallic: false)
        let ink = UnlitMaterial(color: UIColor(white: 0.07, alpha: 1))
        add(cylinder(radius: 0.98, bottom: -0.68, top: 0), material: wood, name: "shell")
        add(cylinder(radius: 0.97, bottom: 0.008, top: 0.025), material: ivory, name: "head")
        add(torus(major: 1, minor: 0.027, y: 0.025), material: metal, name: "upper_hoop")
        add(torus(major: 1, minor: 0.027, y: -0.66), material: metal, name: "lower_hoop")
        let floor = SimpleMaterial(color: UIColor(red: 0.76, green: 0.73, blue: 0.68, alpha: 1), roughness: 1, isMetallic: false)
        add(.generateBox(size: [4.8, 0.02, 4.8]), material: floor, name: "floor", position: [0, -0.76, 0])
        // Geometry for each stave adds a modest grain cue to the material.
        for i in 0..<40 {
            let angle = Float(i) * 2 * .pi / 40
            let strip = ModelEntity(mesh: .generateBox(size: [0.005, 0.62, 0.002]), materials: [wood])
            strip.position = [0.982 * cos(angle), -0.33, 0.982 * sin(angle)]
            strip.orientation = simd_quatf(angle: -angle, axis: [0, 1, 0])
            drum.addChild(strip)
        }
        let text = MeshResource.generateText("TomTune", extrusionDepth: 0.0002,
                                             font: .boldSystemFont(ofSize: 0.10), containerFrame: .zero,
                                             alignment: .center, lineBreakMode: .byClipping)
        let logo = ModelEntity(mesh: text, materials: [ink])
        // Text's local +y points towards noon once laid flat on the membrane.
        logo.orientation = simd_quatf(angle: -.pi / 2, axis: [1, 0, 0])
        let bounds = logo.visualBounds(relativeTo: logo)
        logo.position = [-bounds.center.x, 0.034, -0.65]
        logo.name = "TomTune"
        drum.addChild(logo)
        for index in 0..<lugCount {
            let angle = -Float.pi / 2 + 2 * .pi * (Float(index) + 0.5) / Float(lugCount)
            let position = SIMD3<Float>(1.035 * cos(angle), 0, 1.035 * sin(angle))
            let cap = add(cylinder(radius: 0.055, bottom: 0.04, top: 0.12, segments: 12), material: metal,
                          name: "lug_\(index)", position: position)
            cap.generateCollisionShapes(recursive: false)
            caps.append(cap)
            let rod = add(cylinder(radius: 0.018, bottom: -0.57, top: 0.055, segments: 12), material: metal,
                          name: "rod_\(index)", position: position)
            rod.generateCollisionShapes(recursive: false)
            let halo = add(torus(major: 0.082, minor: 0.013, y: 0.10, segments: 32), material: selected,
                           name: "selection_\(index)", position: position)
            halos.append(halo)
        }
        applySelection()
    }

    private func applySelection() {
        for (index, halo) in halos.enumerated() { halo.isEnabled = index == selectedLug }
        for (index, cap) in caps.enumerated() { cap.model?.materials = [index == selectedLug ? selected : metal] }
    }

    @objc private func tapped(_ gesture: UITapGestureRecognizer) {
        guard gesture.state == .ended else { return }
        let point = gesture.location(in: arView)
        if let entity = arView.entity(at: point), let index = lugIndex(entity.name) {
            onSelect?(index)
            return
        }
        // A 44 pt minimum target around the projected cap keeps small physical screws tappable.
        let candidates = caps.enumerated().compactMap { index, cap -> (Int, CGFloat)? in
            guard let projected = arView.project(cap.convert(position: [0, 0.08, 0], to: nil)) else { return nil }
            let distance = hypot(projected.x - point.x, projected.y - point.y)
            return distance <= 22 ? (index, distance) : nil
        }
        if let nearest = candidates.min(by: { $0.1 < $1.1 }) { onSelect?(nearest.0) }
    }

    private func lugIndex(_ name: String) -> Int? {
        for prefix in ["lug_", "rod_", "selection_"] where name.hasPrefix(prefix) {
            if let index = Int(name.dropFirst(prefix.count)), index >= 0 && index < lugCount { return index }
        }
        return nil
    }

    @discardableResult
    private func add(_ mesh: MeshResource, material: any Material, name: String,
                     position: SIMD3<Float> = .zero) -> ModelEntity {
        let entity = ModelEntity(mesh: mesh, materials: [material])
        entity.name = name
        entity.position = position
        drum.addChild(entity)
        return entity
    }

    private func mesh(_ positions: [SIMD3<Float>], _ normals: [SIMD3<Float>], _ indices: [UInt32]) -> MeshResource {
        var descriptor = MeshDescriptor()
        descriptor.positions = MeshBuffers.Positions(positions)
        descriptor.normals = MeshBuffers.Normals(normals)
        descriptor.primitives = .triangles(indices)
        // Procedural arrays are fixed and checked by tests of the matching generator.
        return try! MeshResource.generate(from: [descriptor])
    }

    private func cylinder(radius: Float, bottom: Float, top: Float, segments: Int = 64) -> MeshResource {
        var positions: [SIMD3<Float>] = [], normals: [SIMD3<Float>] = [], indices: [UInt32] = []
        for i in 0..<segments {
            let a = Float(i) * 2 * .pi / Float(segments)
            let b = Float(i + 1) * 2 * .pi / Float(segments)
            var start = UInt32(positions.count)
            for (angle, y) in [(a, bottom), (a, top), (b, top), (b, bottom)] {
                positions.append([radius * cos(angle), y, radius * sin(angle)])
                normals.append([cos(angle), 0, sin(angle)])
            }
            indices += [0, 1, 2, 0, 2, 3].map { start + $0 }
            for (y, normal, order): (Float, SIMD3<Float>, [UInt32]) in [(top, [0, 1, 0], [0, 2, 1]), (bottom, [0, -1, 0], [0, 1, 2])] {
                start = UInt32(positions.count)
                positions += [[0, y, 0], [radius * cos(a), y, radius * sin(a)], [radius * cos(b), y, radius * sin(b)]]
                normals += [normal, normal, normal]
                indices += order.map { start + $0 }
            }
        }
        return mesh(positions, normals, indices)
    }

    private func torus(major: Float, minor: Float, y: Float, segments: Int = 80, sides: Int = 8) -> MeshResource {
        var positions: [SIMD3<Float>] = [], normals: [SIMD3<Float>] = [], indices: [UInt32] = []
        for i in 0...segments {
            let a = Float(i) * 2 * .pi / Float(segments)
            for j in 0...sides {
                let b = Float(j) * 2 * .pi / Float(sides)
                positions.append([(major + minor * cos(b)) * cos(a), y + minor * sin(b), (major + minor * cos(b)) * sin(a)])
                normals.append([cos(b) * cos(a), sin(b), cos(b) * sin(a)])
            }
        }
        for i in 0..<segments {
            for j in 0..<sides {
                let k = UInt32(i * (sides + 1) + j), s = UInt32(sides)
                indices += [k, k + 1, k + s + 2, k, k + s + 2, k + s + 1]
            }
        }
        return mesh(positions, normals, indices)
    }
}
