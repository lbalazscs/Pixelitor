/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor. If not, see <http://www.gnu.org/licenses/>.
 */

package pixelitor.utils;

import java.util.Random;

/**
 * K-means clustering algorithm for RGB color quantization.
 */
public class KMeansClustering {
    private final int numClusters;
    private final int maxIterations;
    private final Random random;

    public KMeansClustering(int numClusters, int maxIterations, Random random) {
        this.numClusters = numClusters;
        this.maxIterations = maxIterations;
        this.random = random;
    }

    /**
     * Performs color quantization for a fixed number of iterations.
     */
    public int[] cluster(int[] pixels, boolean useKMeansPlusPlus, ProgressTracker pt) {
        // initialize centroids using either k-means++ or random selection
        int[] centroids = useKMeansPlusPlus
            ? initializeKMeansPlusPlusCentroids(pixels)
            : initializeRandomCentroids(pixels);

        pt.unitDone();

        // perform k-means iterations
        for (int it = 0; it < maxIterations; it++) {
            // assign each pixel to the nearest centroid
            IntList[] clusters = assignPixelsToClusters(pixels, centroids);

            // calculate new centroids as the average of assigned colors
            int[] newCentroids = recalculateCentroids(clusters);

            // a convergence check could be added here, but for simplicity
            // we run for a fixed number of iterations.
            // if (Arrays.equals(centroids, newCentroids)) {
            //     break;
            // }

            centroids = newCentroids;
            pt.unitDone();
        }

        return centroids;
    }

    /**
     * Initializes centroids by randomly selecting pixels from the input image.
     */
    private int[] initializeRandomCentroids(int[] pixels) {
        int[] centroids = new int[numClusters];
        for (int i = 0; i < numClusters; i++) {
            centroids[i] = (pixels[random.nextInt(pixels.length)]);
        }
        return centroids;
    }

    /**
     * Initializes centroids using the k-means++ algorithm for better initial placement.
     */
    private int[] initializeKMeansPlusPlusCentroids(int[] pixels) {
        int[] centroids = new int[numClusters];

        // choose the first centroid randomly
        centroids[0] = pixels[random.nextInt(pixels.length)];

        // choose the remaining centroids based on a weighted probability distribution
        for (int i = 1; i < numClusters; i++) {
            double[] distances = new double[pixels.length];
            double sumDistances = calculateKMeansPlusPlusWeights(pixels, i, centroids, distances);

            // choose the next centroid
            selectNextCentroidFromWeights(pixels, sumDistances, distances, centroids, i);
        }

        return centroids;
    }

    /**
     * Calculates distance-based weights for k-means++ initialization.
     */
    private static double calculateKMeansPlusPlusWeights(int[] pixels, int numExistingCentroids, int[] centroids, double[] distances) {
        double sumDistances = 0;
        for (int j = 0; j < pixels.length; j++) {
            // for each pixel, find the squared distance to the nearest existing centroid
            double minSquaredDistance = Double.MAX_VALUE;
            for (int l = 0; l < numExistingCentroids; l++) {
                double squaredDistance = calcSquaredRgbDistance(pixels[j], centroids[l]);
                if (squaredDistance < minSquaredDistance) {
                    minSquaredDistance = squaredDistance;
                }
            }
            distances[j] = minSquaredDistance;
            sumDistances += distances[j];
        }
        return sumDistances;
    }

    /**
     * Selects the next centroid using weighted probability based on distances.
     */
    private void selectNextCentroidFromWeights(int[] pixels, double sumDistances, double[] distances, int[] centroids, int centroidIndex) {
        double randValue = random.nextDouble() * sumDistances;
        for (int j = 0; j < pixels.length; j++) {
            randValue -= distances[j];
            if (randValue <= 0) {
                centroids[centroidIndex] = pixels[j];
                return;
            }
        }
        // fallback for the last pixel, in case of floating point inaccuracies
        centroids[centroidIndex] = pixels[pixels.length - 1];
    }

    /**
     * Assigns each pixel to its nearest centroid cluster.
     */
    private IntList[] assignPixelsToClusters(int[] pixels, int[] centroids) {
        IntList[] clusters = new IntList[numClusters];
        int estimatedSize = pixels.length / numClusters + 1;
        for (int i = 0; i < numClusters; i++) {
            clusters[i] = new IntList(estimatedSize);
        }
        for (int rgb : pixels) {
            int closestCentroidIndex = findClosestCentroidIndex(rgb, centroids);
            clusters[closestCentroidIndex].add(rgb);
        }
        return clusters;
    }

    /**
     * Calculates new centroid positions as the average of all colors in each cluster.
     */
    private int[] recalculateCentroids(IntList[] clusters) {
        int[] newCentroids = new int[numClusters];

        for (int i = 0; i < numClusters; i++) {
            IntList cluster = clusters[i];
            int clusterSize = cluster.size();
            if (clusterSize == 0) {
                // if a cluster is empty, its centroid remains at black.
                // another strategy could be to re-initialize it randomly.
                newCentroids[i] = 0;
                continue;
            }

            // calculate average RGB values for the cluster
            long sumR = 0;
            long sumG = 0;
            long sumB = 0;
            for (int j = 0; j < clusterSize; j++) {
                int rgb = cluster.get(j);
                sumR += (rgb >> 16) & 0xFF;
                sumG += (rgb >> 8) & 0xFF;
                sumB += rgb & 0xFF;
            }
            int newR = (int) (sumR / clusterSize);
            int newG = (int) (sumG / clusterSize);
            int newB = (int) (sumB / clusterSize);
            newCentroids[i] = (newR << 16) | (newG << 8) | newB;
        }
        return newCentroids;
    }

    /**
     * Finds the index of the closest centroid to the given RGB color.
     */
    public static int findClosestCentroidIndex(int rgb, int[] centroids) {
        int closestIndex = 0;
        double closestDistance = Double.MAX_VALUE;

        for (int i = 0; i < centroids.length; i++) {
            double distance = calcSquaredRgbDistance(rgb, centroids[i]);
            if (distance < closestDistance) {
                closestIndex = i;
                closestDistance = distance;
            }
        }
        return closestIndex;
    }

    /**
     * Calculates the squared Euclidean distance between two RGB colors.
     */
    private static double calcSquaredRgbDistance(int rgb1, int rgb2) {
        int r1 = (rgb1 >> 16) & 0xFF;
        int g1 = (rgb1 >> 8) & 0xFF;
        int b1 = rgb1 & 0xFF;

        int r2 = (rgb2 >> 16) & 0xFF;
        int g2 = (rgb2 >> 8) & 0xFF;
        int b2 = rgb2 & 0xFF;

        int dR = r1 - r2;
        int dG = g1 - g2;
        int dB = b1 - b2;

        return (double) dR * dR + dG * dG + dB * dB;
    }
}
