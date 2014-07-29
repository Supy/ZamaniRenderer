package utils;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

public class NormalsCalculator {
    public static float[] calculateFrom(float[] vertices, int[] faces) {
        if(vertices.length % 3 !=0) {
            throw new RuntimeException("number of vertices must be a multiple of 3");
        }

        int[] contributions = new int[vertices.length / 3];
        float[] normals = new float[vertices.length];

        for(int i=0; i < faces.length; i+= 3) {
            int v1Index = faces[i] * 3;
            int v2Index = faces[i+1] * 3;
            int v3Index = faces[i+2] * 3;

            // Reconstruct vertices in 3D space.
            Vector3D v1 = new Vector3D(vertices[v1Index], vertices[v1Index+1], vertices[v1Index+2]);
            Vector3D v2 = new Vector3D(vertices[v2Index], vertices[v2Index+1], vertices[v2Index+2]);
            Vector3D v3 = new Vector3D(vertices[v3Index], vertices[v3Index+1], vertices[v3Index+2]);

            // Need two edges of the triangle to do the cross product.
            Vector3D edge1 = v2.subtract(v3);
            Vector3D edge2 = v1.subtract(v3);

            double[] normalD = edge2.crossProduct(edge1).toArray();
            float[] normalF = new float[] {(float) normalD[0], (float)normalD[1], (float)normalD[2]};

            // Need to normalize the normal else we'll get poorly weighted final normal when we average it.
            float length = (float) Math.sqrt(normalF[0] * normalF[0] + normalF[1] * normalF[1] + normalF[2] * normalF[2]);
            normalF[0] /= length;
            normalF[1] /= length;
            normalF[2] /= length;

            // Add this normal to each of the face's vertices' current sum. We'll average it out later.
            normals[v1Index] += normalF[0];
            normals[v1Index + 1] += normalF[1];
            normals[v1Index + 2] += normalF[2];

            normals[v2Index] += normalF[0];
            normals[v2Index + 1] += normalF[1];
            normals[v2Index + 2] += normalF[2];

            normals[v3Index] += normalF[0];
            normals[v3Index + 1] += normalF[1];
            normals[v3Index + 2] += normalF[2];

            contributions[faces[i]]++;
            contributions[faces[i+1]]++;
            contributions[faces[i+2]]++;
        }

        for(int i=0; i < normals.length; i+=3){
            float contribution = contributions[1];
            normals[i] /= contribution;
            normals[i+1] /= contribution;
            normals[i+2] /= contribution;

            // Normalize the average.
            float length = (float) Math.sqrt(normals[i] * normals[i] + normals[i+1] * normals[i+1] + normals[i+2] * normals[i+2]);
            normals[i] /= length;
            normals[i+1] /= length;
            normals[i+2] /= length;
        }

        return normals;
    }
}
