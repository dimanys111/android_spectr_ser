package com.example.dima.ser;


public class MyVector
{
    byte[] array;//construct a zero length array.

    public MyVector(){
        array = new byte[0];
    }
    //этот метод добавляет новый элемент в конец
    public void add(byte element) {
        byte[] temp = new byte[array.length+1];//each time i add i define a temp that's 1 larger,
        System.arraycopy(array, 0, temp, 0, array.length);
        temp[temp.length-1]=element;	// copy, and set the last element:
        this.array = temp;
    }
    //здесь проблема, элемент добавляется и элементы массива сдвигиваются, но последующие элементы исчезают

    public void add(int index, byte element) {
        if (index > array.length || index < 0) {
            System.out.println("ArrayIndexOutOfBoundsException : index is out of range (index < 0 || index > size())");
        }
        byte[] temp = new byte[array.length + 1]; // здесь надо было увеличить размер массива, ты же добавляешь

        // здесь у тебя неправильное условие итератора i<=index;, отсюда и нули в окончание массива

        for (int i = 0; i < array.length; i++) {
            if (i < index) { // если текущий элемент меньше индекса вставки
                temp[i] = array[i];  // то просто копируем
            } else { //иначе копируем с сдвигом
                temp[i + 1] = array[i];
            }
        }
        temp[index] = element;
        this.array = temp;
    }
}