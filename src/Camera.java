import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationOrder;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

class Camera {

    public static final float NEAR_PLANE = 1f;
    public static final float FAR_PLACE = 2000f;
    public static final float FOV = 45f;

    private final double MOVE_SPEED = 10;

    private Vector3D position;
    private Vector3D direction;

    private double pitch, yaw;

    public Camera() {
        this.position = new Vector3D(-1000, 0, 0);
        this.direction = Vector3D.PLUS_I;

        this.pitch = 0;
        this.yaw = 0;
    }

    private void calculateDirectionVectors() {
        Rotation rotation = new Rotation(RotationOrder.ZYZ, 0, Math.toRadians(this.yaw), Math.toRadians(this.pitch));
        this.direction = rotation.applyTo(Vector3D.PLUS_I).normalize();
    }

    public void addPitch(double angle) {
        this.pitch += angle;

        if (this.pitch >= 90) {
            this.pitch = 89.9;
        } else if (this.pitch <= -89.9) {
            this.pitch = -89.9;
        }

        calculateDirectionVectors();
    }

    public void addYaw(double angle) {
        this.yaw += angle;
        this.yaw %= 360;
        calculateDirectionVectors();
    }

    public Vector3D getPosition() {
        return new Vector3D(this.position.toArray());
    }

    @SuppressWarnings("SameReturnValue")
    public Vector3D getUp() {
        return Vector3D.PLUS_J;
    }

    public Vector3D getLookAt() {
        return this.position.add(this.direction);
    }

    public void moveForward() {
        this.position = this.position.add(MOVE_SPEED, this.direction);
    }

    public void moveBackward() {
        this.position = this.position.subtract(MOVE_SPEED, this.direction);
    }

    public void moveLeft() {
        this.position = this.position.add(MOVE_SPEED, this.direction.crossProduct(Vector3D.MINUS_J).normalize());
    }

    public void moveRight() {
        this.position = this.position.add(MOVE_SPEED, this.direction.crossProduct(Vector3D.PLUS_J).normalize());
    }

    public void moveUp() {
        this.position = this.position.add(MOVE_SPEED, Vector3D.PLUS_J);
    }

}
