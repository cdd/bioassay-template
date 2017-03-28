package com.cdd.bao.util;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;

//extension of HashMap to support multiple keys

public class Bag extends LinkedHashMap{
	public List getValues(Object key){
		if(super.containsKey(key)){
			return(List) super.get(key);
		}else{
			return new ArrayList();
		}
	}
	//once I have the bag
	// I will have the ontology class as key
	// then, and then all the values will be the property-obj pairs
	//the idea is for us to once we have a value chosen, get the key and
	//use the key to get all the values related with it and give it as suggestions
	
	 public Object get(Object key) {
		    ArrayList values = (ArrayList) super.get(key);
		    if (values != null && !values.isEmpty()) {
		      return values.get(0);
		    } else {
		      return null;
		    }
		  }
		  
		  public Object geti(Object key, int i) {
			    ArrayList values = (ArrayList) super.get(key);
			    if (values != null && !values.isEmpty()) {
			      return values.get(i);
			    } else {
			      return null;
			    }
			  }

		  public boolean containsValue(Object value) {
		    return values().contains(value);
		  }

		  public Iterator keySetIter(){
			  Iterator keyIterator = super.keySet().iterator();
			return keyIterator;  
		  }
		  public int size() {
		    int size = 0;
		    Iterator keyIterator = super.keySet().iterator();

		    while (keyIterator.hasNext()) {
		      ArrayList values = (ArrayList) super.get(keyIterator.next());
		      size = size + values.size();
		    }

		    return size;
		  }

		  public Object put(Object key, Object value) {
		    ArrayList values = new ArrayList();

		    if (super.containsKey(key)) {
		      values = (ArrayList) super.get(key);
		      values.add(value);

		    } else {
		      values.add(value);
		    }

		    super.put(key, values);

		    return null;
		  }

		  

		  public Collection values() {
		    List values = new ArrayList();
		    Iterator keyIterator = super.keySet().iterator();

		    while (keyIterator.hasNext()) {
		      List keyValues = (List) super.get(keyIterator.next());
		      values.addAll(keyValues);
		    }

		    return values;
		  }

}
