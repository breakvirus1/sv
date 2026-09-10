# Stage 1: Build
FROM node:20 AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci --no-audit --no-fund
COPY . .
ARG VITE_API_URL
ARG VITE_KEYCLOAK_ISSUER
ARG VITE_KEYCLOAK_CLIENT_ID
ARG SERVER_IP
ENV VITE_API_URL=${VITE_API_URL}
ENV VITE_KEYCLOAK_ISSUER=${VITE_KEYCLOAK_ISSUER}
ENV VITE_KEYCLOAK_CLIENT_ID=${VITE_KEYCLOAK_CLIENT_ID}
RUN npm run build

# Stage 2: Serve with nginx
FROM nginx:alpine AS production
RUN apk add --no-cache gettext
COPY nginx.conf /etc/nginx/conf.d/default.conf.template
COPY --from=builder /app/dist /usr/share/nginx/html
EXPOSE 5174
CMD envsubst '${SERVER_IP}' < /etc/nginx/conf.d/default.conf.template > /etc/nginx/conf.d/default.conf && nginx -g 'daemon off;'

# Stage 3: Development
FROM node:20 AS development
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
ARG SERVER_IP
ENV VITE_SERVER_IP=${SERVER_IP}
EXPOSE 5174
CMD ["npm", "run", "dev", "--", "--host"]
