/**
 *  Copyright 2020 Lolcutus
 *
 *  Version v1.0.0.0000
 
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
		
		command "resetBatteryReplacedDate"
		
		attribute "batteryLastReplaced", "Date"
		attribute "lastUnknownMsg", "String"

		fingerprint profileId: "0104", inClusters: "0000,0003,0019,FFFF,0012", outClusters: "0000,0004,0003,0005,0019,FFFF,0012", manufacturer: "LUMI", model: "lumi.remote.b1acn01"
	}

	preferences {
		input(name: "debugLogging", type: "bool", title: "Enable debug logging", description: "" , defaultValue: false, submitOnChange: true, displayDuringSetup: false, required: false)
		input(name: "infoLogging", type: "bool", title: "Enable info logging", description: "", defaultValue: true, submitOnChange: true, displayDuringSetup: false, required: false)
	}
}

private setVersion(){
	def map = [:]
 	map.name = "driver"
	map.value = "v1.0.0.0000"
	debugLog(map)
	updateDataValue(map.name,map.value)
 }
 def configure() {  
 	setVersion()
 	if(batteryLastReplaced == null){
		 resetBatteryReplacedDate()
	}
 }

	
// Parse incoming device messages to generate events
def parse(String description) {
	//init
	def MODEL = "0000_0005"
	def BATTERY01 = "0000_FF01"
	// init end
	
	debugLog("Parsing ${description}")
	def  msgMap = zigbee.parseDescriptionAsMap(description)
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
			map.name = "model"
			map.value = valueHex
			updateDataValue(map.name, map.value)
			if(map.value == "lumi.remote.b1acn01"){
				updateDataValue("manufacturer", "Lumi")
				updateDataValue("modelName", "Aqara Wireless Mini Switch")
				updateDataValue("modelCode", "WXKG11LM")
				updateDataValue("physicalButtons", "1")
			}
			setVersion()
			break
		case BATTERY01:
			if(msgMap["encoding"] == "42") {
				msgMap = zigbee.parseDescriptionAsMap(description.replace('encoding: 42', 'encoding: 41'))
			}
			debugLog("After change encoding: "+ msgMap)  
			map = parseBattery(msgMap["value"])
			break
		default:
			map.name = "lastUnknownMsg"
			map.value = msgMap
			warnLog("Message not procesed: ${msgMap}")
	}
	infoLog(map)
   	return map
}

private parseBattery(value) {
    def batteryVoltajeFirstIndex = 6 
    def batteryVoltajeSecondIndex = 5
    
    def batteryVoltaje = value[batteryVoltajeFirstIndex .. (batteryVoltajeFirstIndex+1)] + value[batteryVoltajeSecondIndex .. (batteryVoltajeSecondIndex+1)]
    def rawVolts = Integer.parseInt(batteryVoltaje,16)/1000
    
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