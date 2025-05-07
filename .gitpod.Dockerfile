# You can find the new timestamped tags here: https://hub.docker.com/r/gitpod/workspace-full/tags
FROM gitpod/workspace-full:latest

# Install custom tools, runtime, etc.
RUN wget https://raw.githubusercontent.com/snieda/termos/main/termos.sh
RUN source termos.sh -y
