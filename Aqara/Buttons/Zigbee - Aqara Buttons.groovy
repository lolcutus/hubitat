/**
 *  Copyright 2020 Lolcutus
 *
 *  Version v1.0.0.0002
 
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */

metadata {
	definition (name: "Zigbee - Aqara Buttons", namespace: "lolcutus", author: "lolcutus", importUrl: "https://raw.githubusercontent.com/lolcutus/hubitat/master/Aqara/Buttons/Zigbee%20-%20Aqara%20Buttons.groovy") {
		capability "Battery"
		capability "Configuration"
		capability "DoubleTapableButton"
		capability "HoldableButton"
		capability "PushableButton"
        capability "ReleasableButton"
		
		command "resetBatteryReplacedDate"
		
		attribute "batteryLastReplaced", "Date"
		attribute "lastUnknownMsg", "String"

		fingerprint endpointId : "01", profileId: "0104", deviceId: "059D", inClusters: "0000,0012,0003", outClusters: "0000", manufacturer: "LUMI", model: "lumi.remote.b1acn01", aplication:"02"
        fingerprint endpointId : "01", profileId: "0104", deviceId: "01A1", inClusters: "0000,0003,0019,FFFF,0012", outClusters: "0000,0004,0003,0005,0019,FFFF,0012", manufacturer: "LUMI", model: "lumi.remote.b186acn01", aplication:"09"
        fingerprint endpointId : "01", profileId: "0104", deviceId: "01A1", inClusters: "0000,0003,0019,FFFF,0012", outClusters: "0000,0004,0003,0005,0019,FFFF,0012", manufacturer: "LUMI", model: "lumi.remote.b286acn01", aplication:"09"
	}

	preferences {
		input(name: "debugLogging", type: "bool", title: "Enable debug logging", description: "" , defaultValue: false, submitOnChange: true, displayDuringSetup: false, required: false)
		input(name: "infoLogging", type: "bool", title: "Enable info logging", description: "", defaultValue: true, submitOnChange: true, displayDuringSetup: false, required: false)
	}
}

private setVersion(){
	def map = [:]
 	map.name = "driver"
	map.value = "v1.0.0.0002"
	debugLog(map)
	updateDataValue(map.name,map.value)
 }
 def configure() {  
    def map = setDataForModels()
    sendEvent(map)
 	setVersion()
 	if(batteryLastReplaced == null){
		 resetBatteryReplacedDate()
	}
	state.remove("prefsSetCount")
	state.remove("forcedMinutes")
	state.remove("numOfButtons")
}

	
// Parse incoming device messages to generate events
def parse(String description) {
  	//init
	def MODEL = "0000_0005"
	def BATTERY01 = "0000_FF01"
    def BUTTON01 = "0012_0055"
    def NOTKNOWN = "0000_FFF0"
	// init end
	debugLog("Parsing ${description}")
    def  msgMap
    if(description.indexOf('attrId: FF01, encoding: 42') >= 0) {
        msgMap = zigbee.parseDescriptionAsMap(description.replace('encoding: 42', 'encoding: F2'))
        msgMap["encoding"] = "41"
    }else{
        msgMap = zigbee.parseDescriptionAsMap(description)
    }
	
	debugLog(msgMap)
	if(!msgMap.containsKey("cluster")){
		return [:]
	}
	def cluster = msgMap["cluster"]
	def attrId = msgMap["attrId"]
	def encoding = Integer.parseInt(msgMap["encoding"],16)
	def valueHex = msgMap["value"]
	Map map = [:]
	
	def command = msgMap["cluster"] + '_' + msgMap["attrId"]
	debugLog("Command: ${command}")
	switch(command) {
		case MODEL:
          	map = setDataForModels()
			setVersion()
			break
		case BATTERY01:
			map = parseBattery(msgMap["value"])
			break
        case BUTTON01:
            map = parseButtonMessage(msgMap)
            break
        case NOTKNOWN:
            warnLog("Not knowing what to do with ${msgMap}")
            break
		default:
			map.name = "lastUnknownMsg"
			map.value = msgMap
			warnLog("Message not procesed: ${msgMap}")
	}
	infoLog(map)
   	return map
}

private setDataForModels(){
    def map = [:]
    def model = getDataValue("model");
    if(model.length() > "lumi.remote.b1acn01".length() && model.startsWith("lumi.remote.b1acn01")){
        model =  "lumi.remote.b1acn01"
		updateDataValue("model", model)
    }
    if(model.length() > "lumi.remote.b186acn01".length() && model.startsWith("lumi.remote.b186acn01")){
        model =  "lumi.remote.b186acn01"
		updateDataValue("model", model)
    }
    if(model.length() > "lumi.remote.b286acn01".length() && model.startsWith("lumi.remote.b286acn01")){
        model =  "lumi.remote.b286acn01"
		updateDataValue("model", model)
    }
    state.comment = "Works with model WXKG11LM, WXKG03LM, WXKG02LM"
    debugLog("Model '${model}'")
    switch(model){
	    case "lumi.remote.b1acn01":
            debugLog("Configure ${model}")
            if(getDataValue("manufacturer") == null){
			    updateDataValue("manufacturer", "Lumi")
		    }
	        updateDataValue("modelName", "Aqara Wireless Mini Switch")
	        updateDataValue("modelCode", "WXKG11LM")
		    updateDataValue("physicalButtons", "1")
            map.name = "numberOfButtons"
		    map.value = 1
            break
        case "lumi.remote.b186acn01":
            debugLog("Configure ${model}")
		    if(getDataValue("manufacturer") == null){
			    updateDataValue("manufacturer", "Lumi")
		    }
	        updateDataValue("modelName", "Aqara 1-button Light Switch (WXKG03LM - 2018)")
	        updateDataValue("modelCode", "WXKG03LM")
		    updateDataValue("physicalButtons", "1")
            map.name = "numberOfButtons"
		    map.value = 1
            break
        case "lumi.remote.b286acn01":
            debugLog("Configure ${model}")
		    if(getDataValue("manufacturer") == null){
			    updateDataValue("manufacturer", "Lumi")
		    }
	        updateDataValue("modelName", "Aqara 2-button Light Switch (WXKG02LM - 2018)")
	        updateDataValue("modelCode", "WXKG02LM")
		    updateDataValue("physicalButtons", "3")
            map.name = "numberOfButtons"
		    map.value = 3
            break
	}
    map
    
}

private parseButtonMessage(msgMap) {
    
    def buttonNum =  Integer.parseInt(msgMap.endpoint)
    debugLog("Button ${buttonNum}")
    def existingButtons = getDataValue("physicalButtons")
    if(buttonNum > existingButtons ){
        warnLog("Unexpected button. this model has ${existingButtons} buttons but pressed was ${buttonNum}.")
        return []
    }
    def eventType = Integer.parseInt(msgMap.value[2..3],16)
	debugLog("EventType ${eventType}")
    
    def map = [:]
    switch (eventType){
        case 1:
            map.name ="pushed" 
            break
        case 0:
        map.name ="held" 
            break
        case 2:
            map.name ="doubleTapped" 
            break
        case 255:
        map.name ="released" 
            break
        
    }
    map.value = buttonNum
    map
}

private parseBattery(value) {
    def batteryVoltajeFirstIndex
    def batteryVoltajeSecondIndex
    def model = getDataValue("model");
    switch(model){
	    case "lumi.remote.b1acn01":
        case "lumi.remote.b186acn01":
        case "lumi.remote.b286acn01":
            batteryVoltajeFirstIndex = 8 
            batteryVoltajeSecondIndex = 6
            break
	}
    def batteryVoltaje = value[batteryVoltajeFirstIndex .. (batteryVoltajeFirstIndex+1)] + value[batteryVoltajeSecondIndex .. (batteryVoltajeSecondIndex+1)]
    debugLog("batteryVoltaje: " + batteryVoltaje)
    def rawVolts = Integer.parseInt(batteryVoltaje,16)/1000
    debugLog("rawVolts: " + rawVolts)
    def minVolts = 2.5
	def maxVolts = 3.0
	def pct = (rawVolts - minVolts) / (maxVolts - minVolts)
	def roundedPct = Math.min(100, Math.round(pct * 100))
	def descText = "Battery level is ${roundedPct}% (${rawVolts} Volts)"
	
    def map = [:]
    map.name = "battery"
	map.value= roundedPct
	map.unit = "%"
	map.descriptionText = descText
	map
	
	
}

private resetBatteryReplacedDate() {
	sendEvent(name: "batteryLastReplaced", value: new Date())
}

def debugLog(msg){
	if(debugLogging == true){
		log.debug "["+device.getLabel() + "] " + msg
	}
}

def infoLog(msg){
	if(infoLogging == true){
		log.info "[" + device.getLabel() + "] " + msg
	}
}
def warnLog(msg){
	log.warn "[" + device.getLabel() + "] " + msg
}

def traceLog(msg){
	log.trace "[" + device.getLabel() + "] " + msg
}