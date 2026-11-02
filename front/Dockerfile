FROM nginx:alpine AS production
COPY nginx.conf /etc/nginx/conf.d/default.conf.template
COPY dist /usr/share/nginx/html
EXPOSE 5174
CMD sh -c 'sed "s|\${SERVER_IP}|${SERVER_IP}|g" /etc/nginx/conf.d/default.conf.template > /etc/nginx/conf.d/default.conf && exec nginx -g "daemon off;"'
