package com.example.demo.configs;

import com.example.demo.models.Photo;
import com.example.demo.repositories.PhotoRepository;

import java.util.Date;
import java.util.List;

/**
 * Created by student on 7/13/17.
 */
public class MergeSort {

    public static List<Photo> sortPhotos(List<Photo> photoList, PhotoRepository photoRepository){
        int[] sortlist =  new int[photoList.size()];
        int counter = 0;
        for(Photo p : photoList) {
            Date d = p.getCreatedAt();
            int dayVal = d.getDate() + d.getYear() * 365;
            for (int i = d.getMonth(); i > 0; i--){
                if (i == 1) {
                    dayVal += 28;
                } else if ((i % 2 == 0 && i <= 6) || (i % 2 == 1 && i > 6)) {
                    dayVal += 31;
                } else {
                    dayVal += 30;
                }
            }
            sortlist[counter] = dayVal;
            counter++;
        }
        mergesort(sortlist);
        for(int i : sortlist){
            Date d = new Date();
            d.setYear(i%365);
           // if(d - 31 )
        }
        return photoList;
    }

    private static void mergesort(int arr[]) {
        int n = arr.length;
        if (n < 2)
            return;
        int mid = n / 2;
        int left[] = new int[mid];
        int right[] = new int[n - mid];
        for (int i = 0; i < mid; i++)
            left[i] = arr[i];
        for (int i = mid; i < n; i++)
            right[i - mid] = arr[i];
        mergesort(left);
        mergesort(right);
        merge(arr, left, right);
    }

    private static void merge(int arr[], int left[], int right[]) {
        int nL = left.length;
        int nR = right.length;
        int i = 0, j = 0, k = 0;
        while (i < nL && j < nR) {
            if (left[i] <= right[j]) {
                arr[k] = left[i];
                i++;
            } else {
                arr[k] = right[i];
                j++;
            }
            k++;
        }
        while (i < nL) {
            arr[k] = left[i];
            i++;
            k++;
        }
        while (j < nR) {
            arr[k] = right[j];
            j++;
            k++;
        }
    }

}
