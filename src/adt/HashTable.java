
package adt;

public class HashTable<K, V> implements Dictionary<K, V>{
    
    private class Entry<K, V> {
        private K key;
        private V value;
        private Entry<K, V> next;
        
        public Entry(K key, V value){
        this.key = key;
        this.value = value;
        this.next = null;
    }
        public K getKey(){
            return this.key;
        }
        
        public V getValue(){
            return this.value;
        }
        
        public void setValue(V value){
            this.value = value;   
        }
        
        @Override
        public String toString(){
            Entry<K, V> temp = this;
            StringBuilder sb = new StringBuilder();
            while(temp!=null) {
                sb.append(temp.key + "->" + temp.value + ",");
                temp = temp.next;
            }
            return sb.toString();
        }
    }
    
    private final int SIZE = 5;
    
    private Entry<K, V> table[];
    private int size;
    
    public HashTable(){
        table = new Entry[SIZE];
        size = 0;
    }
    
    @Override
    public V add(K key, V value){
        int hash = Math.abs(key.hashCode()) % SIZE;
        Entry<K, V> e = table[hash];
        
        //empty bucket
        if(e == null){
            table[hash] = new Entry<K, V>(key, value);
            size++;
            return null;
        }
        
        while(true){
            if(e.getKey().equals(key)){
                V oldValue = e.getValue();
                e.setValue(value);
                return oldValue;
            }
            if(e.next == null){
                break;
            }
            e = e.next;
        }
        //Add new entry
        e.next = new Entry<K,V>(key, value);
        size++;
        
        return null;
    }  
        
    @Override
    public V getValue(K key){
        int hash = Math.abs(key.hashCode()) % SIZE;
        Entry<K, V> e = table[hash];
        
        while(e != null){
            if(e.getKey().equals(key)){
                return e.getValue();
            }
            e = e.next;
        }
        return null;
    }
        
    @Override
    public V remove(K key){
        int hash = Math.abs(key.hashCode()) % SIZE;
        Entry<K, V> e = table[hash];
        
        if(e==null){
            return null;
        }
        
        if(e.getKey().equals(key)){
            table[hash] = e.next;
            size--;
            return  e.getValue();
        }
        
        Entry<K, V> prev = e;
        e = e.next;
        
        while(e != null){
            if(e.getKey().equals(key)){
                prev.next = e.next;
                size--;
                return e.getValue();
            }
            prev = e;
            e = e.next;
        }
        return null;
    }
    
    @Override
    public boolean contains(K key){
        return getValue(key) != null;
    }
    
    @Override
    public boolean isEmpty(){
        return size == 0;
    }
    
    @Override
    public boolean isFull(){
        return size >= SIZE;
    }
    
    @Override
    public int getSize(){
        return size;
    }
    
     @Override
    public void clear(){
        for(int i = 0; i < SIZE; i++){
            table[i] = null;
        }
        size =0;
    }
    
    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < SIZE; i++){
            if(table[i] != null){
                sb.append(i + "" + table[i] + "\n");
            }else{
                sb.append(i + "" + "null" + "\n");
            }
        }
        return sb.toString();
    }
    
    @Override
    public Object[] getValues(){
        Object[] values = new Object[size];
        int index = 0;
        
        for(int i = 0; i < SIZE; i++){
            Entry<K,V> current = table[i];
            
            while(current != null){
                values[index++] = current.getValue();
                current = current.next;
            }
        }
        return values;
    }
}
