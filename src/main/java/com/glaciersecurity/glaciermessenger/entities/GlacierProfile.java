package com.glaciersecurity.glaciermessenger.entities;


public class GlacierProfile{

	private String name;
	private String uuid;

	public GlacierProfile(String name, String uuid) {
		this.name = name;
		this.uuid = uuid;
	}

	public String getName() {
		return name;
	}

	public String getUuid() {
		return uuid;
	}

//        public String getParcedName(){
//            try {
//                String [] splitname = name.split("_");
//                return splitname[1];
//            } catch (Exception e){
//            }
//            return name;
//
//        }

	@Override
	public String toString() {
		return getName();
	}


}
