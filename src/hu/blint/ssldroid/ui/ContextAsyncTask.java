package hu.blint.ssldroid.ui;

import android.os.AsyncTask;

import java.lang.ref.WeakReference;

public abstract class ContextAsyncTask<Context, Params, Progress, Result> extends AsyncTask<Params, Progress, Result> {
    private final WeakReference<Context> ref;
    public ContextAsyncTask(final Context context) {
        this.ref = new WeakReference<Context>(context);
    }

    @Override
    protected final Result doInBackground(Params... params) {
        final Context context = ref.get();
        return context == null ? null : doInBackground(context, params);
    }

    protected abstract Result doInBackground(Context context, Params... params);

    @Override
    protected final void onPreExecute() {
        final Context context = ref.get();
        if (context != null) {
            onPreExecute(context);
        }
    }

    protected void onPreExecute(Context context) {
    }

    @Override
    protected final void onPostExecute(Result result) {
        final Context context = ref.get();
        if (context != null) {
            onPostExecute(context, result);
        }
    }

    protected void onPostExecute(Context context, Result result) {
    }
}
