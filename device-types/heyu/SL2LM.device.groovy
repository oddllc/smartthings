/**
 *  HeyU SL2LM  Standard Lamp Module 2 (status)
 *
 *  Copyright 2015 Anthony Plack
 *
 *  Monitor your SL2LM using SmartThings and heyU
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
 
import groovy.json.JsonSlurper

preferences {
        input("ip", "string", title:"IP Address", description: "192.168.1.150", required: true, displayDuringSetup: true)
        input("port", "string", title:"Port", description: "3000", defaultValue: 3000 , required: true, displayDuringSetup: true)
        input("devicename", "string", title:"Username", description: "webiopi", required: true, displayDuringSetup: true)
}

metadata {
	definition (name: "heyU-SL2LM", namespace: "oddllc/smartthings", author: "Anthony Plack") {
		capability "Polling"
        capability "Switch"
        capability "Actuator"
        capability "Switch Level"
        
        attribute "switch", "boolean"
        attribute "level", "integer"
        
        command "on"
        command "off"
        command "poll"
        command "setLevel"
	}

	simulator {
		// TODO: define status and reply messages here
	}

	tiles {
        standardTile("switch", "device.switch", width: 1, height: 1, canChangeIcon: true) {
			state "off", label: 'Off', icon: "st.Electronics.electronics18", backgroundColor: "#ffffff", nextState: "on"
			state "on", label: 'On', icon: "st.Electronics.electronics18", backgroundColor: "#79b821", nextState: "off"
		}
        valueTile("level", "device.switchLevel", inactiveLabel: false) {
        	state "default", label:'${currentValue}%', unit:"Percentage",
            backgroundColors:[
                [value: 31, color: "#153591"],
                [value: 44, color: "#1e9cbb"],
                [value: 59, color: "#90d2a7"],
                [value: 74, color: "#44b621"],
                [value: 84, color: "#f1d801"],
                [value: 95, color: "#d04e00"],
                [value: 96, color: "#bc2323"]
            ]
        }
        main "switch"
        details(["switch", "level"])
    }
}

// ------------------------------------------------------------------

// parse events into attributes
def parse(String description) {
    def map = [:]
    def descMap = parseDescriptionAsMap(description)
    log.debug descMap
    def body = new String(descMap["body"].decodeBase64())
    log.debug "body: ${body}"
    def slurper = new JsonSlurper()
    def result = slurper.parseText(body)
    log.debug "result: ${result}"
    if (result.containsKey("onState")) {
    	sendEvent(name: "switch", value: result.onState)
    }  
}

// handle commands
def poll() {
	log.debug "Executing 'poll'"
    getRPiData()
}

def on() {
	log.debug "Executing 'on'"
	def uri = "/heyu/" + devicename + "/on"
    postAction(uri)
}

def off() {
	log.debug "Executing 'off'"
	def uri = "/heyu/" + devicename + "/off"
    postAction(uri)
}

def setLevel() {
	log.debug "Executing 'setLevel'"
	def uri = "/heyu/" + devicename + "/setLevel"
    postAction(uri)
}

// Get on/off status
private getRPiData() {
	def uri = "/heyu/" + devicename + "/onStatus"
    postAction(uri)
}

// ------------------------------------------------------------------

private postAction(uri){
  setDeviceNetworkId(ip,port)  
  
  //def userpass = encodeCredentials(username, password)
  
  //def headers = getHeader(userpass)
  def headers = getHeader("")
  
  def hubAction = new physicalgraph.device.HubAction(
    method: "GET",
    path: uri,
    headers: headers
  )//,delayAction(1000), refresh()]
  log.debug("Executing hubAction on " + getHostAddress())
  log.debug hubAction
  hubAction    
}

// ------------------------------------------------------------------
// Helper methods
// ------------------------------------------------------------------

def parseDescriptionAsMap(description) {
	description.split(",").inject([:]) { map, param ->
		def nameAndValue = param.split(":")
		map += [(nameAndValue[0].trim()):nameAndValue[1].trim()]
	}
}

private encodeCredentials(username, password){
	log.debug "Encoding credentials"
	def userpassascii = "${username}:${password}"
    def userpass = "Basic " + userpassascii.encodeAsBase64().toString()
    //log.debug "ASCII credentials are ${userpassascii}"
    //log.debug "Credentials are ${userpass}"
    return userpass
}

private getHeader(userpass){
	log.debug "Getting headers"
    def headers = [:]
    headers.put("HOST", getHostAddress())
    //headers.put("Authorization", userpass)
    //log.debug "Headers are ${headers}"
    return headers
}

private delayAction(long time) {
	new physicalgraph.device.HubAction("delay $time")
}

private setDeviceNetworkId(ip,port){
  	def iphex = convertIPtoHex(ip)
  	def porthex = convertPortToHex(port)
  	device.deviceNetworkId = "$iphex:$porthex"
  	log.debug "Device Network Id set to ${iphex}:${porthex}"
}

private getHostAddress() {
	return "${ip}:${port}"
}

private String convertIPtoHex(ipAddress) { 
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
    return hex

}

private String convertPortToHex(port) {
	String hexport = port.toString().format( '%04x', port.toInteger() )
    return hexport
}
