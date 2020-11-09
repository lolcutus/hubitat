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
	definition (name: "Zigbee - Aqara Contact", namespace: "lolcutus", author: "lolcutus") {
		capability "Battery"
		capability "Configuration"
		
		attribute "version", "String"
		attribute "batteryLastReplaced", "String"
		attribute "lastUnknownMsg", "String"

		fingerprint endpointId: "01", profileId: "0104", deviceId: "5F01", inClusters: "0000,0003,FFFF,0006", outClusters: "0000,0004,FFFF", manufacturer: "LUMI", model: "lumi.sensor_magnet.aq2"
	}

	preferences {
		input(name: "debugLogging", type: "bool", title: "Enable debug logging", description: "" , defaultValue: false, submitOnChange: true, displayDuringSetup: false, required: false)
		input(name: "infoLogging", type: "bool", title: "Enable info logging", description: "", defaultValue: true, submitOnChange: true, displayDuringSetup: false, required: false)
	}
}

private setVersion(){
	def map = [:]
 	map.name = "version"
	map.value = "v1.0.0.0002"
	debugLog(map)
	sendEvent(map)
 }
 def configure() {  
    setVersion()
 }

	
// Parse incoming device messages to generate events
def parse(String description) {
    //init
    def MODEL = "0000_0005"
    def CONTACT = "0006_0000"
    def BATTERY01 = "0000_FF01"
    //end Init
	//debugLog("Parsing ${description}")
    def  msgMap = zigbee.parseDescriptionAsMap(description)
	debugLog(msgMap)
	if(!msgMap.containsKey("cluster")){
		return [:]
	}
	def valueHex = msgMap["value"]
	Map map = [:]
	def command = msgMap["cluster"] + '_' + msgMap["attrId"]
	debugLog("Command: ${command}")
	switch(command) {
		case MODEL:
			map.model = valueHex
			updateDataValue('model', valueHex)
			break
		case CONTACT:
			map = parseContact(Integer.parseInt(valueHex))
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
                
private parseContact(closedOpen) {
    debugLog("Value ${closedOpen}")
    def map = [:]
	map.name = "contact"
    if(closedOpen == 0){
        map.value = "closed"
    }else{
        map.value = "open"
    }
	map.descriptionText = "Contact was ${map.value}!"
    map
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
private parseBattery2(value) {
    def batteryVoltajeFirstIndex = 7 
    def batteryVoltajeSecondIndex = 5
    
    def bateryVoltaje = value[batteryVoltajeFirstIndex .. (batteryVoltajeFirstIndex+1)] + value[batteryVoltajeSecondIndex .. (batteryVoltajeSecondIndex+1)]
    
    
	displayDebugLog("Battery voltaje = ${bateryVoltaje}")
	def MsgLength = description.size()
	
	def rawVolts = rawValue / 1000
	def minVolts = voltsmin ? voltsmin : 2.5
	def maxVolts = voltsmax ? voltsmax : 3.0
	def pct = (rawVolts - minVolts) / (maxVolts - minVolts)
	def roundedPct = Math.min(100, Math.round(pct * 100))
	def descText = "Battery level is ${roundedPct}% (${rawVolts} Volts)"
	displayInfoLog(descText)
	def result = [
		name: 'battery',
		value: roundedPct,
		unit: "%",
		isStateChange: true,
		descriptionText: descText
	]
	return result
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