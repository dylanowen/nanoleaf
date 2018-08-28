# Nanoleaf

# Debian Build
1. `docker run -v $(pwd):/root --rm dylanowen/sbt-packager-debian sbt debian:packageBin`
2. `scp target/bart-pi_0.2_all.deb pi@raspberrypi.local:~/`
3. `ssh pi@raspberrypi.local`
4. `sudo apt install ./bart-pi_0.2_all.deb`
5. `JAVA_OPTS="-Dbart-pi.station=12th -Dbart-pi.lines.0=RED -Dbart-pi.lines.1=YELLOW -Dbart-pi.direction=s" bash -c "bart-pi"`

# Simple Build
`sbt assembly`

# Push to Raspberry Pi
1. `scp target/scala-2.12/nanoleaf-assembly-0.1.jar pi@raspberrypi.local:~/nanoleaf`
2. `java -jar nanoleaf-assembly-0.1.jar &` to start in the background
3. `pkill -f 'nanoleaf\-assembly'` to kill the process

## Sites:
http://www.firewall.cx/networking-topics/protocols/domain-name-system-dns/160-protocols-dns-query.html
https://github.com/posicks/mdnsjava/tree/6f304c4f4a36b92d3a73be31820bbd9f02615a8f/mdnsjava/src/main/java/net/posick/mDNS
https://doc.akka.io/docs/akka/2.5/io-udp.html