/*
 * This is a shim to provide usual PAC functions using the PacFunctions Java class. 
 */
var PacFunctions = Java.type('io.mh0rst.net.pacproxy.PacFunctions');

function log(what) {
	return PacFunctions.log(what);
}

function dnsDomainIs(host, domain) {
	return PacFunctions.dnsDomainIs(host, domain);
}

function dnsDomainLevels(host) {
	return PacFunctions.dnsDomainLevels(host);
}

function dnsResolve(host) {
	return PacFunctions.dnsResolve(host);
}

function isPlainHostName(host) {
	return PacFunctions.isPlainHostName(host);
}

function isResolvable(host) {
	return PacFunctions.isResolvable(host);
}

function isInNet(host, pattern, mask) {
	return PacFunctions.isInNet(host, pattern, mask);
}

function localHostOrDomainIs(host, hostdom) {
	return PacFunctions.localHostOrDomainIs(host, hostdom);
}

function myIpAddress() {
	return PacFunctions.myIpAddress();
}

function shExpMatch(str, shexp) {
	return PacFunctions.shExpMatch(str, shexp);
}

// Start of PAC extensions
// https://blogs.msdn.microsoft.com/wndp/2006/07/13/extensions-to-the-navigator-proxy-auto-config-file-format-specification-to-support-ipv6-v0-9/

function isResolvableEx(host) {
	return PacFunctions.isResolvable(host);
}

function isInNetEx(address, prefix) {
	// TODO
}

function dnsResolveEx(host) {
	// TODO
}

function myIPAddressEx() {
	return PacFunctions.myIpAddressEx();
}

function sortIpAddressList(addressList) {
	// TODO
}

function getClientVersion() {
	return 1.0;
}